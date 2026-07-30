#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
container_engine="${CONTAINER_ENGINE:-podman}"
image_prefix="${LINUX_COMPATIBILITY_IMAGE_PREFIX:-localhost/pocketportal-linux-compatibility}"
distributions=(debian fedora)

for distribution in "${distributions[@]}"; do
  "$container_engine" build \
    --file "$project_dir/testing/linux-compatibility/Containerfile.$distribution" \
    --tag "$image_prefix-$distribution:latest" \
    "$project_dir"
done
