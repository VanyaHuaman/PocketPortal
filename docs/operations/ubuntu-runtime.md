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

The runtime layout and service template have been validated on a real Ubuntu host. The resumable production installer, prerequisite doctor, versioned upgrade command, and automated rollback are still under development.
