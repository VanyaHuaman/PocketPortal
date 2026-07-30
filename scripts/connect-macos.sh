#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
connect_binary="$project_dir/connect/build/install/pocketportal-connect/bin/pocketportal-connect"
default_local_port=15556
default_config_dir="${XDG_CONFIG_HOME:-$HOME/.config}/pocketportal"
default_ca_file="$default_config_dir/pocketportal-ca.pem"
remote_ca_file=".config/pocketportal/tls/pocketportal-ca.pem"
remote_environment_file=".config/pocketportal/pocketportal.env"
token_variable_name="POCKETPORTAL_ADB_BRIDGE_TOKEN"
keychain_service="dev.pocketportal.connect"

server="${POCKETPORTAL_CONNECT_SERVER:-}"
ssh_target="${POCKETPORTAL_CONNECT_SSH_TARGET:-}"
serial=""
device_name=""
local_port="$default_local_port"
adb_path="${POCKETPORTAL_CONNECT_ADB:-}"
ca_file="${POCKETPORTAL_CONNECT_CA_CERTIFICATE:-$default_ca_file}"

usage() {
  cat <<EOF
Usage:
  $0 --server wss://HOST:PORT --ssh-target USER@HOST
     [--device FRIENDLY_NAME | --serial DEVICE_SERIAL]
     [--local-port PORT] [--adb PATH] [--ca-certificate PATH]

The first run uses SSH to copy the PocketPortal certificate and stores the
bridge token in the current macOS user's login Keychain. Later runs reuse them.

You may set POCKETPORTAL_CONNECT_SERVER and POCKETPORTAL_CONNECT_SSH_TARGET
instead of passing --server and --ssh-target each time.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server)
      server="${2:-}"
      shift 2
      ;;
    --ssh-target)
      ssh_target="${2:-}"
      shift 2
      ;;
    --serial)
      serial="${2:-}"
      shift 2
      ;;
    --device)
      device_name="${2:-}"
      shift 2
      ;;
    --local-port)
      local_port="${2:-}"
      shift 2
      ;;
    --adb)
      adb_path="${2:-}"
      shift 2
      ;;
    --ca-certificate)
      ca_file="${2:-}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

[[ "$(uname -s)" == "Darwin" ]] || {
  echo "This launcher currently supports macOS only." >&2
  exit 1
}
[[ -n "$server" ]] || {
  echo "--server or POCKETPORTAL_CONNECT_SERVER is required." >&2
  exit 1
}
[[ -n "$ssh_target" ]] || {
  echo "--ssh-target or POCKETPORTAL_CONNECT_SSH_TARGET is required." >&2
  exit 1
}
[[ -z "$serial" || -z "$device_name" ]] || {
  echo "Use either --device or --serial, not both." >&2
  exit 1
}
[[ "$local_port" =~ ^[0-9]+$ ]] || {
  echo "--local-port must be numeric." >&2
  exit 1
}

find_adb() {
  local candidate
  local candidates=()

  if [[ -n "$adb_path" ]]; then
    candidates+=("$adb_path")
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    candidates+=("$ANDROID_SDK_ROOT/platform-tools/adb")
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    candidates+=("$ANDROID_HOME/platform-tools/adb")
  fi
  candidates+=(
    "$HOME/Library/Android/sdk/platform-tools/adb"
    "/Applications/Android Studio.app/Contents/sdk/platform-tools/adb"
  )
  if command -v adb >/dev/null 2>&1; then
    candidates+=("$(command -v adb)")
  fi

  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  echo "ADB was not found. Install Android Studio Platform Tools or pass --adb PATH." >&2
  return 1
}

if [[ ! -x "$connect_binary" ]]; then
  echo "Building PocketPortal Connect..."
  "$project_dir/gradlew" -p "$project_dir" :connect:installDist
fi

resolved_adb="$(find_adb)"
mkdir -p "$(dirname "$ca_file")"

if [[ ! -f "$ca_file" ]]; then
  echo "Copying the PocketPortal certificate from $ssh_target..."
  scp "$ssh_target:$remote_ca_file" "$ca_file"
  chmod 600 "$ca_file"
fi

select_device() {
  local api_server
  local inventory_file
  local selected
  local selection
  local devices=()

  api_server="${server/#wss:/https:}"
  inventory_file="$(mktemp "${TMPDIR:-/tmp}/pocketportal-devices.XXXXXX")"
  trap 'rm -f "$inventory_file"' EXIT
  if ! curl --fail --silent \
    --cacert "$ca_file" \
    "$api_server/api/devices" >"$inventory_file"; then
    # Managed Macs may terminate TLS with an employer CA installed in curl's
    # normal trust configuration instead of presenting PocketPortal's CA.
    curl --fail --silent --show-error \
      "$api_server/api/devices" >"$inventory_file"
  fi

  while IFS= read -r selected; do
    devices+=("$selected")
  done < <(
    osascript -l JavaScript \
      "$project_dir/scripts/connect-device-picker.js" \
      "$inventory_file" \
      "$device_name"
  )

  if [[ -n "$device_name" ]]; then
    if [[ "${#devices[@]}" -ne 1 ]]; then
      echo "No unique online device matches '$device_name'." >&2
      return 1
    fi
    serial="${devices[0]%%	*}"
    return
  fi

  [[ "${#devices[@]}" -gt 0 ]] || {
    echo "PocketPortal has no online Android devices." >&2
    return 1
  }
  echo "PocketPortal devices:"
  local index
  for ((index = 0; index < ${#devices[@]}; index++)); do
    printf '  %d. %s\n' "$((index + 1))" "${devices[$index]#*	}"
  done
  printf "Select a device [1-%d]: " "${#devices[@]}"
  read -r selection
  [[ "$selection" =~ ^[0-9]+$ ]] &&
    ((selection >= 1 && selection <= ${#devices[@]})) || {
    echo "Invalid device selection." >&2
    return 1
  }
  serial="${devices[$((selection - 1))]%%	*}"
}

if [[ -z "$serial" ]]; then
  select_device
fi

token="$(security find-generic-password \
  -a "$server" \
  -s "$keychain_service" \
  -w 2>/dev/null || true)"

if [[ -z "$token" ]]; then
  echo "Saving the PocketPortal Connect token in your login Keychain..."
  token="$(ssh "$ssh_target" \
    "sed -n 's/^${token_variable_name}=//p' '${remote_environment_file}'")"
  [[ -n "$token" ]] || {
    echo "The PocketPortal bridge token was not found on $ssh_target." >&2
    exit 1
  }
  security add-generic-password \
    -U \
    -a "$server" \
    -s "$keychain_service" \
    -w "$token" >/dev/null
fi

echo "Using ADB: $resolved_adb"
echo "Connecting $serial through $server. Press Ctrl+C to disconnect."

export POCKETPORTAL_CONNECT_TOKEN="$token"
exec "$connect_binary" \
  --server "$server" \
  --serial "$serial" \
  --local-port "$local_port" \
  --adb "$resolved_adb" \
  --ca-certificate "$ca_file"
