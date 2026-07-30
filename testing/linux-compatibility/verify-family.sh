#!/usr/bin/env bash
set -euo pipefail

expected_family_message="${1:?Expected family message is required}"
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
installer="$project_dir/scripts/install-linux.sh"
output_file="$(mktemp)"

cleanup() {
  rm -f -- "$output_file"
}
trap cleanup EXIT

if "$installer" install \
  --archive "$project_dir/testing/linux-compatibility/missing.tar" \
  --version compatibility-test >"$output_file" 2>&1; then
  echo "Expected the prerequisite check to fail in the minimal container." >&2
  exit 1
fi

grep --fixed-strings --quiet -- "has not been verified" "$output_file"
grep --fixed-strings --quiet -- "Required command is missing: java" "$output_file"
grep --fixed-strings --quiet -- "$expected_family_message" "$output_file"

if grep --fixed-strings --quiet -- "supports Ubuntu only" "$output_file"; then
  echo "Installer unexpectedly rejected a non-Ubuntu Linux distribution." >&2
  exit 1
fi

echo "Linux compatibility check passed: $expected_family_message"
