# Getting started

PocketPortal currently provides the backend foundation and Android device discovery. The web dashboard, authentication, leases, and application installation are planned but not implemented yet.

## Requirements

- Java 17 or newer
- Git
- ADB for live Android discovery
- Podman only when running the optional clean-room installation test

## Build and test

```bash
git clone https://github.com/VanyaHuaman/PocketPortal.git
cd PocketPortal
./scripts/bootstrap.sh
```

The bootstrap checks Java, uses the checked-in Gradle wrapper, builds every module, and runs the automated test suite.

## Run

```bash
./gradlew :app:run
```

PocketPortal listens on the configured address. The default development endpoint is:

```text
http://localhost:8080/api/status
```

Continue with the [development setup](development.md) or review [configuration](../configuration.md).
