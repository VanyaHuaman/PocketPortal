#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
installer_config="$project_dir/deploy/linux/install.env"

if [[ ! -f "$installer_config" ]]; then
  echo "Installer configuration is missing: $installer_config" >&2
  exit 1
fi

set -a
source "$installer_config"
set +a

data_dir="${XDG_DATA_HOME:-$HOME/.local/share}/pocketportal"
config_dir="${XDG_CONFIG_HOME:-$HOME/.config}/pocketportal"
systemd_dir="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user"
binary_dir="$HOME/.local/bin"
releases_dir="$data_dir/releases"
current_link="$data_dir/current"
binary_link="$binary_dir/pocketportal"
service_file="$systemd_dir/$POCKETPORTAL_SERVICE_NAME"
config_file="$config_dir/pocketportal.properties"
archive=""
version=""
operation="install"
previous_target=""
staging_dir=""
host_distribution_id=""
host_distribution_version=""
host_distribution_family=""

usage() {
  echo "Usage:"
  echo "  $0 install --archive PATH --version VERSION"
  echo "  $0 rollback --version VERSION"
}

cleanup() {
  if [[ -n "$staging_dir" && -d "$staging_dir" ]]; then
    rm -rf -- "$staging_dir"
  fi
}
trap cleanup EXIT

if [[ $# -gt 0 && ("$1" == "install" || "$1" == "upgrade" || "$1" == "rollback") ]]; then
  operation="$1"
  shift
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --archive)
      archive="${2:-}"
      shift 2
      ;;
    --version)
      version="${2:-}"
      shift 2
      ;;
    --allow-unsupported-os|--allow-untested-linux)
      echo "Note: $1 is no longer required; unverified Linux releases continue with a warning."
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown installer argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command is missing: $1" >&2
    print_prerequisite_guidance
    return 1
  fi
}

matches_family() {
  local candidates="$1"
  local candidate
  for candidate in $candidates; do
    if [[ "$host_distribution_id" == "$candidate" ||
      " $host_distribution_family " == *" $candidate "* ]]; then
      return 0
    fi
  done
  return 1
}

print_prerequisite_guidance() {
  if matches_family "$POCKETPORTAL_DEBIAN_FAMILY_IDS"; then
    echo "Install prerequisites with your Debian-family package manager, for example:" >&2
    echo "  sudo apt install default-jre adb android-sdk-platform-tools-common curl" >&2
  elif matches_family "$POCKETPORTAL_FEDORA_FAMILY_IDS"; then
    echo "Install prerequisites with your Fedora-family package manager, for example:" >&2
    echo "  sudo dnf install java-17-openjdk-headless android-tools curl" >&2
  else
    echo "Install Java $POCKETPORTAL_MINIMUM_JAVA_VERSION+, ADB, curl, tar, and systemd tools using your distribution's package manager." >&2
  fi
}

validate_host() {
  if [[ ! -r /etc/os-release ]]; then
    echo "PocketPortal's host installer requires Linux with /etc/os-release." >&2
    exit 1
  fi

  source /etc/os-release
  host_distribution_id="${ID:-}"
  host_distribution_version="${VERSION_ID:-unknown}"
  host_distribution_family="${ID_LIKE:-}"
  if [[ -z "$host_distribution_id" ]]; then
    echo "The Linux distribution identifier is missing from /etc/os-release." >&2
    exit 1
  fi

  host_release="$host_distribution_id:$host_distribution_version"
  if [[ " $POCKETPORTAL_EOL_LINUX_RELEASES " == *" $host_release "* ]]; then
    echo "Warning: $host_release is end-of-life even though PocketPortal has been verified on it."
    echo "Upgrade the host distribution before relying on unattended operation."
  elif [[ " $POCKETPORTAL_VERIFIED_LINUX_RELEASES " != *" $host_release "* ]]; then
    echo "Warning: $host_release has not been verified by the PocketPortal project."
    echo "Continuing because the portable runtime prerequisites are available."
  fi

  require_command java
  require_command adb
  require_command systemctl
  require_command loginctl
  require_command curl
  require_command tar

  java_major="$(java -version 2>&1 | head -n 1 | sed -E 's/.*version \"([0-9]+).*/\1/')"
  if [[ ! "$java_major" =~ ^[0-9]+$ || "$java_major" -lt "$POCKETPORTAL_MINIMUM_JAVA_VERSION" ]]; then
    echo "Java $POCKETPORTAL_MINIMUM_JAVA_VERSION or newer is required." >&2
    exit 1
  fi

  if ! systemctl --user is-system-running >/dev/null 2>&1; then
    systemd_state="$(systemctl --user is-system-running 2>/dev/null || true)"
    if [[ "$systemd_state" != "degraded" ]]; then
      echo "The user systemd manager is unavailable." >&2
      exit 1
    fi
  fi

  linger_state="$(loginctl show-user "$USER" -p Linger --value 2>/dev/null || true)"
  if [[ "$linger_state" != "yes" ]]; then
    echo "Systemd lingering is required. Run: sudo loginctl enable-linger $USER" >&2
    exit 1
  fi
}

activate_release() {
  local release_path="$1"
  local relative_target="releases/$(basename "$release_path")"

  ln -sfn "$relative_target" "$current_link.next"
  mv -fT "$current_link.next" "$current_link"
  ln -sfn "$current_link/bin/pocketportal" "$binary_link"
  systemctl --user daemon-reload
  systemctl --user enable "$POCKETPORTAL_SERVICE_NAME" >/dev/null
  systemctl --user restart "$POCKETPORTAL_SERVICE_NAME"
}

wait_for_health() {
  local attempt
  for ((attempt = 1; attempt <= POCKETPORTAL_STARTUP_ATTEMPTS; attempt++)); do
    if curl --fail --silent \
      "$POCKETPORTAL_HEALTH_URL$POCKETPORTAL_STATUS_PATH" >/dev/null; then
      curl --fail --silent \
        "$POCKETPORTAL_HEALTH_URL$POCKETPORTAL_DEVICES_PATH" >/dev/null
      return 0
    fi
    sleep "$POCKETPORTAL_STARTUP_DELAY_SECONDS"
  done
  return 1
}

rollback_after_failure() {
  if [[ -n "$previous_target" ]]; then
    ln -sfn "$previous_target" "$current_link.next"
    mv -fT "$current_link.next" "$current_link"
    systemctl --user restart "$POCKETPORTAL_SERVICE_NAME"
    echo "Upgrade failed health checks; restored $previous_target." >&2
  else
    systemctl --user stop "$POCKETPORTAL_SERVICE_NAME" >/dev/null 2>&1 || true
    echo "Initial installation failed health checks; service stopped." >&2
  fi
}

[[ -n "$version" ]] || {
  echo "--version is required." >&2
  usage
  exit 1
}

validate_host
mkdir -p "$releases_dir" "$config_dir" "$systemd_dir" "$binary_dir"

if [[ -L "$current_link" ]]; then
  previous_target="$(readlink "$current_link")"
fi

release_name="pocketportal-$version"
release_path="$releases_dir/$release_name"

if [[ "$operation" == "rollback" ]]; then
  if [[ ! -x "$release_path/bin/pocketportal" ]]; then
    echo "Rollback release is not installed: $release_path" >&2
    exit 1
  fi
  activate_release "$release_path"
  if ! wait_for_health; then
    rollback_after_failure
    exit 1
  fi
  echo "PocketPortal rolled back to $version."
  exit 0
fi

[[ -n "$archive" ]] || {
  echo "--archive is required for installation." >&2
  exit 1
}
[[ -f "$archive" ]] || {
  echo "Distribution archive does not exist: $archive" >&2
  exit 1
}

if [[ ! -x "$release_path/bin/pocketportal" ]]; then
  staging_dir="$(mktemp -d "$releases_dir/.install-XXXXXX")"
  tar -xf "$archive" -C "$staging_dir"
  staged_release="$staging_dir/$release_name"
  if [[ ! -x "$staged_release/bin/pocketportal" ]]; then
    echo "Archive does not contain $release_name/bin/pocketportal." >&2
    exit 1
  fi
  mv "$staged_release" "$release_path"
fi

if [[ ! -f "$config_file" ]]; then
  cp "$project_dir/deploy/linux/pocketportal.properties" "$config_file"
fi
cp "$project_dir/deploy/linux/pocketportal.service" "$service_file"

activate_release "$release_path"
if ! wait_for_health; then
  rollback_after_failure
  exit 1
fi

echo "PocketPortal $version is installed and healthy."
echo "Run: $binary_link doctor"
