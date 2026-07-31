# Linux runtime

PocketPortal's hardware-facing service runs directly on Linux rather than in a production container. The portable runtime requires Java 17 or newer and ADB, but it does not require a JDK or Gradle when installed from a built distribution. The current service adapter requires a user systemd manager.

## Compatibility contract

The core application does not depend on Ubuntu, Debian, Fedora, or a particular package manager. A V1 host must provide:

- Linux with `/etc/os-release`
- Java 17 or newer
- ADB
- `curl` and `tar`
- A user systemd manager and lingering

Ubuntu 25.10 is the first real-host validation, although that distribution release is now end-of-life. Debian-family and Fedora-family package guidance is implemented but still needs disposable-VM verification. Other Linux distributions may run when the same prerequisites are present and are reported as unverified rather than rejected.

Non-systemd Linux hosts will require another service-manager adapter. That is a replaceable infrastructure boundary, not a reason to couple the application to one distribution.

## Initial safety boundary

Before configuring trusted-home-LAN TLS, bind PocketPortal to localhost:

```properties
server.host=127.0.0.1
server.port=8080
android.adb.path=/usr/bin/adb
android.adb.timeoutMillis=5000
android.bridge.enabled=false
```

The repository contains this safe starting configuration in `deploy/linux/pocketportal.properties`.

## Configure home-LAN TLS

After installing a bridge-capable release, generate a host-specific
certificate, PKCS12 key store, bridge token, and owner-only secret environment
file:

```bash
./scripts/configure-linux-tls.sh --host 192.168.0.151
systemctl --user restart pocketportal.service
```

The script keeps plaintext HTTP on `127.0.0.1`, binds HTTPS/WSS only to the
specified private address on port `8443`, enables the limited ADB bridge, and
refuses to overwrite existing TLS material. Copy the generated public
certificate from:

```text
~/.config/pocketportal/tls/pocketportal-ca.pem
```

Never copy `pocketportal.p12` or `pocketportal.env` to a client.

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
~/.config/pocketportal/pocketportal.env
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

Verify locally on the Linux host:

```bash
curl --fail http://127.0.0.1:8080/api/status
curl --fail http://127.0.0.1:8080/api/devices
```

## Accessing the dashboard

**Localhost access** (from the Linux host):

```text
http://127.0.0.1:8080
```

**Private LAN access** (from other devices on your network):

Before TLS configuration:
```text
http://HOST_IP:8080
```

After running `configure-linux-tls.sh`:
```text
https://HOST_IP:8443
```

Replace `HOST_IP` with your server's private LAN address (e.g., `192.168.0.151`).

The dashboard shows:
- Live device inventory with model, Android version, battery, and connection details
- Device screenshots (tap to enlarge)
- Current device state (online, offline, unauthorized)

When connecting from a browser on another machine, you may see a certificate warning since PocketPortal uses a self-signed certificate. This is expected for a private LAN deployment—accept the certificate to continue.

!!! warning
    Bind plaintext HTTP only to loopback. Bind HTTPS/WSS only to a trusted
    private-LAN address protected by the host firewall. PocketPortal has no
    application login in V1; never expose it through router port forwarding or
    a public interface.

## Installation status

Build a distribution on a development or CI machine, then copy the archive and repository deployment files to the Linux host. Install or upgrade with:

```bash
./scripts/install-linux.sh install \
  --archive pocketportal-<version>.tar \
  --version <version>
```

Running the same command again is safe. Existing configuration is preserved. A release is activated only from an executable, versioned distribution, then the installer probes both `/api/status` and `/api/devices`. A failed probe restores the prior release.

Roll back to an already installed version with:

```bash
./scripts/install-linux.sh rollback --version <previous-version>
```

The installer reads `/etc/os-release` for distribution-aware prerequisite guidance, but it does not block an otherwise compatible distribution. Verified releases are listed in `deploy/linux/install.env`; other Linux releases continue with a clear warning while Java, ADB, systemd, lingering, service state, and device access are checked directly.

Debian-family and Fedora-family hosts receive package-manager-specific prerequisite suggestions. Other distributions receive a generic requirements list. Package installation is never performed silently.

The installer, doctor, upgrade, automatic failure recovery, and explicit rollback were validated against the live Ubuntu host with an authorized Pixel 4 XL on July 29, 2026. Ubuntu is the first real-host validation, not a requirement of the application.
