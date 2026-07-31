# Getting started

PocketPortal currently provides a single-owner web dashboard, Android device
discovery and observations, bounded screenshots, a safe wake action, private
home-LAN HTTPS, and the server side of the PocketPortal Connect bridge.
Single-device APK installation is planned next. PocketPortal-managed
authentication, leases, test runners, and off-LAN access are deferred to V2.

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

To connect a server-attached Android device to Android Studio on a Mac, use
[PocketPortal Connect](https://vanyahuaman.github.io/PocketPortal-Connect/).
