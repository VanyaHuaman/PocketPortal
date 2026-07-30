# Clean-room testing

The Podman clean-room harness verifies that PocketPortal does not depend on undeclared state from a developer workstation.

```bash
./testing/clean-room/run.sh
```

The test:

1. Builds from a fresh Linux JDK image.
2. Executes the normal bootstrap and automated tests.
3. Creates an application distribution.
4. Starts PocketPortal as a non-root user in a clean JRE image.
5. Loads an external configuration file.
6. Verifies readiness.
7. Verifies safe behavior when ADB is intentionally absent.
8. Removes the temporary container.

This does not replace a Linux-host installation test. systemd, udev, USB permissions, ADB ownership, scrcpy, upgrades, and rollback require a disposable VM or real host. Ubuntu is currently the first real-host validation.

## Distribution compatibility checks

PocketPortal also builds minimal Debian and Fedora containers around `install-linux.sh`:

```bash
./testing/linux-compatibility/run.sh
```

These checks prove that distribution detection and package-manager guidance work without an Ubuntu-only gate. They intentionally stop at the first missing prerequisite. Containers do not prove user-systemd, lingering, udev, USB, or physical-device behavior; those remain real-host or booted-VM checks.
