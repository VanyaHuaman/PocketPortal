# Ubuntu runtime

PocketPortal's hardware-facing service runs directly on Ubuntu rather than in a production container. The runtime requires Java 17 or newer and ADB, but it does not require a JDK or Gradle when installed from a built distribution.

## Initial safety boundary

Until authentication and private networking are configured, bind PocketPortal to localhost:

```properties
server.host=127.0.0.1
server.port=8080
android.adb.path=/usr/bin/adb
android.adb.timeoutMillis=5000
```

The repository contains this safe starting configuration in `deploy/ubuntu/pocketportal.properties`.

## User service layout

The current user-owned deployment layout is:

```text
~/.local/share/pocketportal/
├── current -> releases/pocketportal-<version>
└── releases/
    └── pocketportal-<version>/
        ├── bin/
        └── lib/

~/.config/pocketportal/pocketportal.properties
~/.config/systemd/user/pocketportal.service
```

The user must have systemd lingering enabled for the service to start without an interactive login.

## Operations

Run host diagnostics:

```bash
~/.local/bin/pocketportal doctor
```

Warnings do not make the command fail; missing required capabilities return a non-zero exit code.

Inspect the service:

```bash
systemctl --user status pocketportal.service
```

Follow logs:

```bash
journalctl --user --unit pocketportal.service --follow
```

Restart after a configuration change:

```bash
systemctl --user restart pocketportal.service
```

Verify locally on the Ubuntu host:

```bash
curl --fail http://127.0.0.1:8080/api/status
curl --fail http://127.0.0.1:8080/api/devices
```

!!! warning
    Do not change the bind address to a LAN or public interface until PocketPortal authentication and the intended private-network controls are in place.

## Installation status

Build a distribution on a development or CI machine, then copy the archive and repository deployment files to the Ubuntu host. Install or upgrade with:

```bash
./scripts/install-ubuntu.sh install \
  --archive pocketportal-<version>.tar \
  --version <version>
```

Running the same command again is safe. Existing configuration is preserved. A release is activated only from an executable, versioned distribution, then the installer probes both `/api/status` and `/api/devices`. A failed probe restores the prior release.

Roll back to an already installed version with:

```bash
./scripts/install-ubuntu.sh rollback --version <previous-version>
```

The installer supports Ubuntu versions listed in `deploy/ubuntu/install.env`. An untested Ubuntu release requires the explicit `--allow-unsupported-os` flag and remains visible as a warning in `pocketportal doctor`.

The installer, doctor, upgrade, automatic failure recovery, and explicit rollback were validated against the live Ubuntu host with an authorized Pixel 4 XL on July 29, 2026.
