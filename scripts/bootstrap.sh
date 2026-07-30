#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
minimum_java_version=17
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

if [[ ! -x "./gradlew" ]]; then
  echo "Gradle wrapper is missing. Install Gradle and run: gradle wrapper --gradle-version $gradle_wrapper_version"
  exit 1
fi

echo "Building and testing PocketPortal..."
./gradlew build
echo "PocketPortal is ready. Start it with: ./gradlew :app:run"
