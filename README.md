# PocketPortal

PocketPortal is a lean, self-hosted control plane for a physical mobile-device lab. The first release targets six Android devices connected to an Ubuntu server.

## Prerequisites

- Java 17 or newer
- Node.js will be required when the frontend is introduced
- ADB and scrcpy will be required when Android integration is introduced

## First setup

```bash
./scripts/bootstrap.sh
```

The bootstrap checks prerequisites, downloads pinned build dependencies through the Gradle wrapper, and runs all current tests.

## Run locally

```bash
./gradlew :app:run
```

Then open <http://localhost:8080/api/status>.

The initial Android discovery endpoint is available at <http://localhost:8080/api/devices>. It returns typed device states such as `online`, `offline`, and `unauthorized`. If ADB is missing, times out, or fails, the endpoint returns a safe service-unavailable response rather than raw process output.

Use a different port with:

```bash
POCKETPORTAL_PORT=9090 ./gradlew :app:run
```

Runtime defaults live in [`config/pocketportal.properties`](./config/pocketportal.properties) and are packaged with the application. Override the config path with `POCKETPORTAL_CONFIG`. Individual overrides currently include `POCKETPORTAL_HOST`, `POCKETPORTAL_PORT`, `POCKETPORTAL_ADB_PATH`, and `POCKETPORTAL_ADB_TIMEOUT_MILLIS`.

On an Ubuntu host, build the distribution elsewhere and install it with:

```bash
./scripts/install-ubuntu.sh install \
  --archive pocketportal-<version>.tar \
  --version <version>
```

The installer preserves configuration, activates an immutable versioned release, verifies both API endpoints, and automatically restores the previous release if health checks fail. Run `~/.local/bin/pocketportal doctor` for host diagnostics. See the [Ubuntu runtime guide](./docs/operations/ubuntu-runtime.md) for upgrades and rollback.

## Verify changes

```bash
./scripts/verify.sh
```

## Clean-room installation test

With an existing Podman machine running:

```bash
./testing/clean-room/run.sh
```

This builds PocketPortal in a fresh Linux JDK image using the normal bootstrap, creates the application distribution, starts it as a non-root user in a smaller JRE image, verifies readiness, and confirms that an intentionally absent ADB executable produces a typed service-unavailable response without preventing startup. The temporary test container is removed automatically.

Clean-room settings live in [`testing/clean-room/clean-room.env`](./testing/clean-room/clean-room.env). This harness validates portable application installation; it does not replace Ubuntu-host testing for systemd, udev, USB, ADB, scrcpy, or physical devices.

## Documentation site

Build the MkDocs Material site with:

```bash
./scripts/docs.sh build
```

Preview it locally with:

```bash
./scripts/docs.sh serve
```

The script creates an isolated `.venv-docs` environment and installs the pinned documentation dependency. GitHub Actions builds the site strictly for pull requests and deploys it to GitHub Pages after documentation changes reach `main`.

## Structure

- `domain`: framework-free business models and rules
- `application`: use cases and external-system ports
- `infrastructure`: adapters for clocks and, later, ADB, SQLite, and processes
- `web`: Ktor transport and API models
- `app`: composition root and executable service
- `frontend`: React application, added with the first dashboard feature
- `testing/clean-room`: Podman-based fresh-Linux installation smoke test
- `docs`: public MkDocs Material documentation
- `deploy/ubuntu`: localhost-only configuration and user-systemd service templates
- `scripts/install-ubuntu.sh`: resumable Ubuntu install, upgrade, health check, and rollback

PocketPortal is a modular monolith. Capabilities move into separate projects only when a concrete host, trust, runtime, lifecycle, scaling, or reuse boundary requires it.

See [`PocketPortal-Plan.md`](./PocketPortal-Plan.md) for the roadmap and [`AI_HANDOFF.md`](./AI_HANDOFF.md) for continuation context.
