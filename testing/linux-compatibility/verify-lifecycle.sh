#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
test_config="$project_dir/testing/linux-compatibility/lifecycle.env"
installer="$project_dir/scripts/install-linux.sh"
fixture_root="$(mktemp -d)"
test_home="$(mktemp -d)"
service_log="$(mktemp)"

cleanup() {
  rm -rf -- "$fixture_root"
  rm -rf -- "$test_home"
  rm -f -- "$service_log"
}
trap cleanup EXIT

set -a
source "$test_config"
set +a

export HOME="$test_home"
export USER=pocketportal-test
export PATH="$project_dir/testing/linux-compatibility/fake-bin:$PATH"
export POCKETPORTAL_TEST_SERVICE_LOG="$service_log"

create_archive() {
  local version="$1"
  local release_name="pocketportal-$version"
  local release_root="$fixture_root/$release_name"
  local executable="$release_root/bin/pocketportal"

  mkdir -p "$release_root/bin"
  printf '#!/usr/bin/env bash\nexit 0\n' >"$executable"
  chmod +x "$executable"
  tar -cf "$fixture_root/$release_name.tar" -C "$fixture_root" "$release_name"
}

install_version() {
  local version="$1"
  "$installer" install \
    --archive "$fixture_root/pocketportal-$version.tar" \
    --version "$version"
}

assert_current_version() {
  local version="$1"
  local current_link="$HOME/.local/share/pocketportal/current"
  local expected_target="releases/pocketportal-$version"

  [[ "$(readlink "$current_link")" == "$expected_target" ]]
  [[ -x "$HOME/.local/bin/pocketportal" ]]
}

create_archive "$LIFECYCLE_FIRST_VERSION"
create_archive "$LIFECYCLE_SECOND_VERSION"
create_archive "$LIFECYCLE_FAILED_VERSION"

install_version "$LIFECYCLE_FIRST_VERSION"
assert_current_version "$LIFECYCLE_FIRST_VERSION"
grep --fixed-strings --quiet -- \
  "$LIFECYCLE_EXPECTED_CONFIG_HOST" \
  "$HOME/.config/pocketportal/pocketportal.properties"

install_version "$LIFECYCLE_FIRST_VERSION"
assert_current_version "$LIFECYCLE_FIRST_VERSION"

install_version "$LIFECYCLE_SECOND_VERSION"
assert_current_version "$LIFECYCLE_SECOND_VERSION"

export POCKETPORTAL_TEST_FAIL_HEALTH=true
if install_version "$LIFECYCLE_FAILED_VERSION"; then
  echo "Expected the simulated unhealthy upgrade to fail." >&2
  exit 1
fi
unset POCKETPORTAL_TEST_FAIL_HEALTH
assert_current_version "$LIFECYCLE_SECOND_VERSION"

"$installer" rollback --version "$LIFECYCLE_FIRST_VERSION"
assert_current_version "$LIFECYCLE_FIRST_VERSION"

grep --fixed-strings --quiet -- \
  "enable $LIFECYCLE_SERVICE_NAME" \
  "$service_log"
grep --fixed-strings --quiet -- \
  "restart $LIFECYCLE_SERVICE_NAME" \
  "$service_log"

echo "Linux installer lifecycle passed."
