#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
connect_binary="$project_dir/connect/build/install/pocketportal-connect/bin/pocketportal-connect"
default_local_port=15556
default_config_dir="${XDG_CONFIG_HOME:-$HOME/.config}/pocketportal"
default_ca_file="$default_config_dir/pocketportal-ca.pem"
client_config_file="$default_config_dir/connect-client.properties"
remote_ca_file=".config/pocketportal/tls/pocketportal-ca.pem"
remote_environment_file=".config/pocketportal/pocketportal.env"
token_variable_name="POCKETPORTAL_ADB_BRIDGE_TOKEN"
keychain_service="dev.pocketportal.connect"

color_reset=$'\033[0m'
color_lime=$'\033[38;5;154m'
color_muted=$'\033[38;5;108m'
color_bright=$'\033[38;5;255m'
color_panel=$'\033[48;5;234m'
cursor_home=$'\033[H'
clear_screen=$'\033[2J'
cursor_hide=$'\033[?25l'
cursor_show=$'\033[?25h'

read_saved_setting() {
  local setting_name="$1"
  [[ -f "$client_config_file" ]] || return 0
  sed -n "s/^${setting_name}=//p" "$client_config_file" | tail -n 1
}

server="${POCKETPORTAL_CONNECT_SERVER:-$(read_saved_setting server)}"
ssh_target="${POCKETPORTAL_CONNECT_SSH_TARGET:-$(read_saved_setting sshTarget)}"
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

The first run bootstraps the certificate and Keychain credential over SSH and
remembers the server. Later, run this script with no options and select a device
with the arrow keys.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server) server="${2:-}"; shift 2 ;;
    --ssh-target) ssh_target="${2:-}"; shift 2 ;;
    --serial) serial="${2:-}"; shift 2 ;;
    --device) device_name="${2:-}"; shift 2 ;;
    --local-port) local_port="${2:-}"; shift 2 ;;
    --adb) adb_path="${2:-}"; shift 2 ;;
    --ca-certificate) ca_file="${2:-}"; shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

[[ "$(uname -s)" == "Darwin" ]] || {
  echo "This launcher currently supports macOS only." >&2
  exit 1
}
[[ -n "$server" ]] || {
  echo "--server is required on the first run." >&2
  exit 1
}
[[ -n "$ssh_target" ]] || {
  echo "--ssh-target is required on the first run." >&2
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

mkdir -p "$default_config_dir"
{
  printf 'server=%s\n' "$server"
  printf 'sshTarget=%s\n' "$ssh_target"
} >"$client_config_file"
chmod 600 "$client_config_file"

find_adb() {
  local candidate
  local candidates=()
  [[ -z "$adb_path" ]] || candidates+=("$adb_path")
  [[ -z "${ANDROID_SDK_ROOT:-}" ]] ||
    candidates+=("$ANDROID_SDK_ROOT/platform-tools/adb")
  [[ -z "${ANDROID_HOME:-}" ]] ||
    candidates+=("$ANDROID_HOME/platform-tools/adb")
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
  echo "ADB was not found. Install Platform Tools or pass --adb PATH." >&2
  return 1
}

if [[ ! -x "$connect_binary" ]]; then
  printf '%s●%s Building PocketPortal Connect...\n' "$color_lime" "$color_reset"
  "$project_dir/gradlew" -p "$project_dir" :connect:installDist
fi
resolved_adb="$(find_adb)"

mkdir -p "$(dirname "$ca_file")"
if [[ ! -f "$ca_file" ]]; then
  printf '%s●%s Installing the PocketPortal certificate...\n' "$color_lime" "$color_reset"
  scp "$ssh_target:$remote_ca_file" "$ca_file"
  chmod 600 "$ca_file"
fi

select_device() {
  local api_server="${server/#wss:/https:}"
  local inventory_file
  local selected
  local selection
  local selected_index=0
  local key
  local escape_suffix
  local index
  local label manufacturer android kind battery charging screen
  local devices=()

  inventory_file="$(mktemp "${TMPDIR:-/tmp}/pocketportal-devices.XXXXXX")"
  trap 'rm -f "$inventory_file"' EXIT

  load_devices() {
    devices=()
    if ! curl --fail --silent --cacert "$ca_file" \
      "$api_server/api/devices" >"$inventory_file"; then
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
  }

  load_devices
  if [[ -n "$device_name" ]]; then
    [[ "${#devices[@]}" -eq 1 ]] || {
      echo "No unique online device matches '$device_name'." >&2
      return 1
    }
    serial="${devices[0]%%	*}"
    return
  fi
  [[ "${#devices[@]}" -gt 0 ]] || {
    echo "PocketPortal has no online Android devices." >&2
    return 1
  }

  if [[ ! -t 0 || ! -t 1 ]]; then
    echo "PocketPortal devices:"
    for ((index = 0; index < ${#devices[@]}; index++)); do
      IFS=$'\t' read -r _ label manufacturer android kind battery charging screen \
        <<<"${devices[$index]}"
      printf '  %d. %s · %s · Android %s\n' \
        "$((index + 1))" "$label" "$kind" "$android"
    done
    printf "Select a device [1-%d]: " "${#devices[@]}"
    read -r selection
    [[ "$selection" =~ ^[0-9]+$ ]] &&
      ((selection >= 1 && selection <= ${#devices[@]})) || {
      echo "Invalid device selection." >&2
      return 1
    }
    serial="${devices[$((selection - 1))]%%	*}"
    return
  fi

  trap 'printf "%s" "$cursor_show"; rm -f "$inventory_file"' EXIT
  printf '%s%s' "$cursor_hide" "$clear_screen"
  while true; do
    printf '%s%s' "$cursor_home" "$clear_screen"
    printf '%s┌─ POCKETPORTAL CONNECT ──────────────────────────────────┐%s\n' \
      "$color_lime" "$color_reset"
    printf '│  %sServer%s  %-34s %s● online%s  │\n' \
      "$color_muted" "$color_reset" "$api_server" "$color_lime" "$color_reset"
    printf '%s├──────────────────────────────────────────────────────────┤%s\n' \
      "$color_muted" "$color_reset"
    for ((index = 0; index < ${#devices[@]}; index++)); do
      IFS=$'\t' read -r _ label manufacturer android kind battery charging screen \
        <<<"${devices[$index]}"
      if [[ "$index" -eq "$selected_index" ]]; then
        printf '│  %s› %-36s%s %s● ONLINE%s  │\n' \
          "$color_bright" "$label" "$color_reset" "$color_lime" "$color_reset"
      else
        printf '│    %-36s %s● ONLINE%s  │\n' \
          "$label" "$color_lime" "$color_reset"
      fi
      printf '│    %s%-52s%s  │\n' "$color_muted" \
        "$manufacturer · Android $android · $kind" "$color_reset"
      printf '│    %s%-52s%s  │\n' "$color_muted" \
        "Battery $battery · $charging · screen $screen" "$color_reset"
      printf '│                                                          │\n'
    done
    printf '%s├──────────────────────────────────────────────────────────┤%s\n' \
      "$color_muted" "$color_reset"
    printf '│  %s↑/↓ select   Enter connect   R refresh   Q quit%s       │\n' \
      "$color_muted" "$color_reset"
    printf '%s└──────────────────────────────────────────────────────────┘%s' \
      "$color_muted" "$color_reset"

    IFS= read -rsn1 key
    case "$key" in
      $'\033')
        IFS= read -rsn2 -t 1 escape_suffix || true
        case "$escape_suffix" in
          "[A")
            selected_index=$(( (selected_index - 1 + ${#devices[@]}) % ${#devices[@]} ))
            ;;
          "[B")
            selected_index=$(( (selected_index + 1) % ${#devices[@]} ))
            ;;
        esac
        ;;
      "")
        serial="${devices[$selected_index]%%	*}"
        printf '%s%s%s' "$cursor_show" "$color_reset" "$clear_screen"
        return
        ;;
      r|R) load_devices; selected_index=0 ;;
      q|Q) printf '%s%s\n' "$cursor_show" "$color_reset"; exit 0 ;;
    esac
  done
}

if [[ -z "$serial" ]]; then
  select_device
fi

token="$(security find-generic-password \
  -a "$server" -s "$keychain_service" -w 2>/dev/null || true)"
if [[ -z "$token" ]]; then
  printf '%s●%s Saving the Connect credential in your login Keychain...\n' \
    "$color_lime" "$color_reset"
  token="$(ssh "$ssh_target" \
    "sed -n 's/^${token_variable_name}=//p' '${remote_environment_file}'")"
  [[ -n "$token" ]] || {
    echo "The PocketPortal bridge token was not found on $ssh_target." >&2
    exit 1
  }
  security add-generic-password -U \
    -a "$server" -s "$keychain_service" -w "$token" >/dev/null
fi

printf '%s✓%s ADB found: %s\n' "$color_lime" "$color_reset" "$resolved_adb"
printf '%s●%s Connecting %s. Press Ctrl+C to disconnect.\n' \
  "$color_lime" "$color_reset" "$serial"

export POCKETPORTAL_CONNECT_TOKEN="$token"
exec "$connect_binary" \
  --server "$server" \
  --serial "$serial" \
  --local-port "$local_port" \
  --adb "$resolved_adb" \
  --ca-certificate "$ca_file"
