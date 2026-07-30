#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
minimum_java_version=17
minimum_node_version=20
gradle_wrapper_version=8.14.4
cd "$project_dir"

if ! command -v java >/dev/null 2>&1; then
  echo "PocketPortal requires Java $minimum_java_version or newer."
  exit 1
fi

java_major="$(java -version 2>&1 | awk -F '[\".]' '/version/ { print $2; exit }')"
if [[ -z "$java_major" || "$java_major" -lt "$minimum_java_version" ]]; then
  echo "PocketPortal requires Java $minimum_java_version or newer; found Java ${java_major:-unknown}."
  exit 1
fi

if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  echo "PocketPortal development requires Node.js $minimum_node_version or newer and npm."
  exit 1
fi

node_major="$(node --version | sed -E 's/^v([0-9]+).*/\1/')"
if [[ -z "$node_major" || "$node_major" -lt "$minimum_node_version" ]]; then
  echo "PocketPortal requires Node.js $minimum_node_version or newer; found ${node_major:-unknown}."
  exit 1
fi

if [[ ! -x "./gradlew" ]]; then
  echo "Gradle wrapper is missing. Install Gradle and run: gradle wrapper --gradle-version $gradle_wrapper_version"
  exit 1
fi

echo "Building and testing PocketPortal..."
./gradlew build
echo "PocketPortal is ready. Start it with: ./gradlew :app:run"
