#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_config="$project_dir/testing/docs-site/docs-site.env"
page_manifest="$project_dir/testing/docs-site/pages.txt"
response_file="$(mktemp)"

cleanup() {
  rm -f -- "$response_file"
}
trap cleanup EXIT

if [[ ! -f "$test_config" || ! -f "$page_manifest" ]]; then
  echo "Documentation site test configuration is incomplete." >&2
  exit 1
fi

set -a
source "$test_config"
set +a

verify_url() {
  local url="$1"
  local marker="${2:-}"
  local attempt

  for ((attempt = 1; attempt <= DOCS_SITE_ATTEMPTS; attempt++)); do
    if curl --fail --location --silent --show-error \
      --output "$response_file" "$url"; then
      if [[ -z "$marker" ]] || grep --fixed-strings --quiet -- "$marker" "$response_file"; then
        echo "PASS $url"
        return 0
      fi
    fi
    sleep "$DOCS_SITE_RETRY_DELAY_SECONDS"
  done

  echo "FAIL $url" >&2
  return 1
}

while IFS= read -r path; do
  [[ -n "$path" ]] || continue
  marker=""
  if [[ "$path" == "/" ]]; then
    marker="$DOCS_SITE_GENERATOR_MARKER"
  fi
  verify_url "$DOCS_SITE_BASE_URL$path" "$marker"
done < "$page_manifest"

verify_url \
  "$DOCS_SITE_BASE_URL$DOCS_SITE_STYLESHEET_PATH" \
  "$DOCS_SITE_STYLESHEET_MARKER"

verify_url "$DOCS_SITE_BASE_URL$DOCS_SITE_SCREENSHOT_PATH"
