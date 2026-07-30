#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
config_file="$project_dir/testing/clean-room/clean-room.env"

if [[ ! -f "$config_file" ]]; then
  echo "Clean-room configuration is missing: $config_file"
  exit 1
fi

set -a
source "$config_file"
set +a

container_name="${CONTAINER_NAME_PREFIX}-$$"
device_response_file="$(mktemp)"

cleanup() {
  podman --connection "$PODMAN_CONNECTION" rm --force "$container_name" >/dev/null 2>&1 || true
  rm -f "$device_response_file"
}
trap cleanup EXIT

podman --connection "$PODMAN_CONNECTION" build \
  --file "$project_dir/testing/clean-room/Containerfile" \
  --tag "$IMAGE_NAME" \
  "$project_dir"

podman --connection "$PODMAN_CONNECTION" run \
  --detach \
  --name "$container_name" \
  --publish "127.0.0.1::${CONTAINER_PORT}" \
  "$IMAGE_NAME" >/dev/null

port_mapping="$(
  podman --connection "$PODMAN_CONNECTION" port \
    "$container_name" \
    "${CONTAINER_PORT}/tcp"
)"
host_port="${port_mapping##*:}"
status_url="http://127.0.0.1:${host_port}${STATUS_PATH}"

for ((attempt = 1; attempt <= STARTUP_ATTEMPTS; attempt++)); do
  if response="$(curl --fail --silent "$status_url")"; then
    if [[ "$response" == *"$EXPECTED_STATUS_FRAGMENT"* ]]; then
      device_http_status="$(
        curl \
          --silent \
          --output "$device_response_file" \
          --write-out "%{http_code}" \
          "http://127.0.0.1:${host_port}${DEVICES_PATH}"
      )"
      device_response="$(<"$device_response_file")"

      if [[ "$device_http_status" != "$EXPECTED_DEVICE_HTTP_STATUS" ]]; then
        echo "Expected device HTTP status $EXPECTED_DEVICE_HTTP_STATUS; got $device_http_status."
        exit 1
      fi

      if [[ "$device_response" != *"$EXPECTED_DEVICE_ERROR_FRAGMENT"* ]]; then
        echo "Unexpected missing-ADB response: $device_response"
        exit 1
      fi

      echo "Clean-room installation test passed: $response"
      echo "Missing-ADB behavior passed: $device_response"
      exit 0
    fi

    echo "PocketPortal responded with an unexpected status: $response"
    exit 1
  fi

  sleep "$STARTUP_DELAY_SECONDS"
done

echo "PocketPortal did not become ready at $status_url."
podman --connection "$PODMAN_CONNECTION" logs "$container_name"
exit 1
