#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "install-ubuntu.sh is retained for compatibility; using install-linux.sh."
exec "$project_dir/scripts/install-linux.sh" "$@"
