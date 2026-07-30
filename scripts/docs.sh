#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
virtual_environment_directory="$project_dir/.venv-docs"
requirements_file="$project_dir/docs/requirements.txt"
command_name="${1:-build}"

if ! command -v python3 >/dev/null 2>&1; then
  echo "PocketPortal documentation requires Python 3."
  exit 1
fi

if [[ ! -d "$virtual_environment_directory" ]]; then
  python3 -m venv "$virtual_environment_directory"
fi

"$virtual_environment_directory/bin/python" -m pip install \
  --disable-pip-version-check \
  --requirement "$requirements_file"

case "$command_name" in
  build)
    "$virtual_environment_directory/bin/mkdocs" build --strict --config-file "$project_dir/mkdocs.yml"
    ;;
  serve)
    "$virtual_environment_directory/bin/mkdocs" serve --config-file "$project_dir/mkdocs.yml"
    ;;
  *)
    echo "Usage: ./scripts/docs.sh [build|serve]"
    exit 1
    ;;
esac
