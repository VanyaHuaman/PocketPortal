# PocketPortal AI Handoff

Last updated: July 30, 2026

## Purpose

This document gives a new AI assistant enough context to continue PocketPortal without reconstructing the full conversation. The detailed and authoritative product plan is in [`PocketPortal-Plan.md`](./PocketPortal-Plan.md). Read that document before making architectural or scope changes.

## Project summary

PocketPortal is a self-hosted mobile device lab for six physical Android
devices and one iPhone. A Linux home server is the always-on Android host; the
user's current machine runs Ubuntu. V1 serves one owner from computers on the
same trusted home network. Shared access, remote access, and managed test
execution are later platform capabilities.

The platform must remain application-project agnostic. PocketPortal does not
model source repositories, projects, workspaces, tenants, users, roles, leases,
test-suite catalogs, or test runs in V1. It is a single-owner personal lab
focused on devices, browser control, and application installation.

## Current status

- Planning is complete enough to begin the proof of concept.
- The Git repository and initial Kotlin modular-monolith scaffold have been created.
- Backend modules are `domain`, `application`, `infrastructure`, `web`, and `app`.
- A minimal `/api/status` vertical slice proves domain-to-use-case-to-web-adapter composition.
- Tests cover the domain model, status use case, Ktor route, and configuration override behavior.
- The checked-in Gradle 8.14.4 wrapper builds successfully with Kotlin 2.4.10 and Ktor 3.5.1.
- Builds use the installed Java 17-or-newer JDK while targeting Java 17-compatible bytecode; do not force provisioning of a separate Java 17 toolchain.
- `./scripts/bootstrap.sh` performs prerequisite checks and runs the complete build.
- A runtime smoke test returned a valid ready response from the configured status endpoint.
- The first Android device-discovery slice is implemented through domain, application, infrastructure, web, and composition-root boundaries.
- `GET /api/devices` parses bounded `adb devices -l` output into typed states and returns safe service-unavailable errors for a missing, timed-out, or failed ADB process.
- Unit and API tests cover serial validation, the discovery use case, ADB parsing, gateway failure mapping, configuration, and web error mapping.
- Live discovery was verified against an attached Android device, including correct reporting of its unauthorized state.
- The first real Ubuntu runtime deployment is installed as an enabled user systemd service under `~/.local/share/pocketportal`.
- The service uses an external config in `~/.config/pocketportal`, binds only to localhost, and is intentionally unreachable through the server's LAN address.
- The authorized Pixel 4 XL is reported online by the live `/api/devices` endpoint.
- `pocketportal doctor` now checks Linux distribution confidence, Java, ADB, connected devices, user systemd, lingering, service state, and current-boot kernel segfaults through tested clean-architecture boundaries.
- `scripts/install-linux.sh` provides distribution-neutral resumable installation and upgrade, immutable version activation, API health checks, automatic restoration after failed health checks, and explicit rollback. The old `install-ubuntu.sh` entrypoint is a compatibility wrapper.
- Version `0.1.9-SNAPSHOT` is active on the real server. Earlier upgrade and rollback behavior was verified between `0.1.0` and `0.1.1`; subsequent dashboard, generalized-Linux, enriched-observation, screenshot, wake-action, limited ADB bridge, and adaptive device-frame releases were deployed through the same versioned upgrade path.
- The installed `~/.local/bin/pocketportal doctor` finds the conventional user config automatically. On the server it reports one authorized Pixel 4 XL, active service, enabled lingering, and zero current-boot segfaults.
- Ubuntu 25.10 is the first real-host validation but is end-of-life, so doctor and the generalized installer emit an upgrade warning.
- Minimal Debian 13 and Fedora 42 container checks validate non-Ubuntu distribution detection, prerequisite guidance, initial installation, idempotent reinstall, versioned upgrade, health-gated activation, and rollback. Service and health commands are deterministic fakes, so these checks do not claim real systemd, udev, USB, or physical-device validation.
- `.github/workflows/verify.yml` runs the complete JVM/frontend suite and the Debian/Fedora installer compatibility matrix on pushes and pull requests.
- The server needs only a Java runtime for artifact deployment. Its source checkout cannot build without a JDK, which confirms that build and runtime prerequisites must remain distinct in the installer and doctor.
- An initial JVM crash occurred on Ubuntu kernel `6.17.0-40` alongside hundreds of unrelated process segfaults. After rebooting into `6.17.0-41`, the complete workload and running service produced zero segfaults. Continue monitoring before declaring the host fully stable.
- `./testing/clean-room/run.sh` builds and tests in a fresh Linux JDK image, assembles the distribution, starts it as a non-root user in a clean JRE image with external configuration, probes readiness, and removes the temporary container.
- The Podman clean-room test passed end to end on July 29, 2026, including graceful missing-ADB behavior.
- A later clean-room rerun was blocked because the existing Podman machine immediately returned to `stopped` after startup; the application build and complete test suite still pass, and the real-host installer path passed.
- A public-facing MkDocs Material site now lives in `docs`, builds strictly with `./scripts/docs.sh build`, and has a GitHub Pages workflow.
- The docs dependency is pinned to MkDocs Material 9.7.7 and remains isolated from the PocketPortal application.
- The documentation site now shares the dashboard's PocketPortal visual language through `docs/stylesheets/pocketportal.css`: dark forest canvas, lime accents, subtle grid texture, rounded panels, system typography, and styled navigation, search, code, tables, and admonitions.
- The docs build passes locally and in GitHub Actions.
- The public repository is `https://github.com/VanyaHuaman/PocketPortal`.
- GitHub Pages uses the Actions workflow and is live at `https://vanyahuaman.github.io/PocketPortal/`.
- The repository Pages setting must remain `build_type: workflow`. It was previously left in legacy `/docs` mode, which caused Jekyll to overwrite the successful MkDocs deployment with raw source and broken routes. This was corrected through the GitHub API on July 29, 2026.
- `scripts/verify-docs-site.sh` checks every public documentation route, the MkDocs generator marker, and the branded stylesheet after each Pages deployment.
- The initial Pages build and deployment completed successfully on July 29, 2026, and both public URLs were verified over HTTPS.
- The first React/TypeScript dashboard slice is implemented and served from the Ktor artifact. It is deliberately read-only, shows responsive cards for `/api/devices`, refreshes every five seconds, and handles loading, empty, and recoverable error states.
- Online device observations now include manufacturer, Android and SDK versions, battery percentage, charging status, screen state, connection type, and observation time. One bounded static ADB shell script collects details per online device; failures preserve the discovered device with partial data.
- The dashboard now requests a bounded PNG screenshot for each online device every 30 seconds. `GET /api/devices/{serial}/screenshot` revalidates the serial against live ADB inventory, uses a fixed screencap argument list, applies the configured timeout and byte limit, and returns typed failures without raw process output.
- The screenshot slice was verified on the live Pixel 4 XL: the endpoint returned a valid 1440×3040 RGBA PNG (305,330 bytes), unknown serials returned `404`, and the upgraded service remained active.
- The first safe device-action slice adds `POST /api/devices/{serial}/actions/wake`. It revalidates live inventory, sends only the fixed named `KEYCODE_WAKEUP` under the configured timeout, and maps offline, missing, timed-out, and failed states without exposing process output.
- The wake action was verified on the live Pixel 4 XL with a `204` response; an unknown serial returned `404`, device discovery remained healthy, and the upgraded user service stayed active.
- A Mac remote-ADB experiment proved that `ADB_SERVER_SOCKET` can reach the server-connected Pixel through an SSH localhost tunnel for CLI commands. Android Studio's ADB `36.0.2` repeatedly terminated the Ubuntu-packaged ADB `34.0.5` daemon. After upgrading the server to the same `36.0.2`, Android Studio still terminated the remote daemon. Direct Android Studio use of the server's ADB smart socket is therefore not a supported client workflow.
- The server now has Google's Platform Tools `36.0.2` isolated at `~/.local/share/pocketportal/tools/platform-tools-36.0.2`; PocketPortal's external config points to that ADB, while `/usr/bin/adb` remains untouched. The downloaded archive SHA-256 was `3afdea91441815ab41254193df0343d92c1b1c0d0237165c3a345c8af8891c31`. Pixel discovery, PocketPortal API health, and doctor checks passed after the switch.
- Future easy client setup must preserve Android Studio's normal local ADB daemon and use a separately proven per-device transport, or guide users to a Linux-hosted IDE through remote desktop. It must not automate `ADB_SERVER_SOCKET` against PocketPortal's shared server daemon.
- The per-device alternative passed end to end on the Pixel 4 XL: server ADB temporarily enabled authenticated TCP mode on device port `5555`; Mac port `5556` was forwarded over SSH to the Pixel's Wi-Fi address; the Mac's normal ADB daemon connected to `127.0.0.1:5556`; the Pixel approved the Mac's distinct ADB key; and Android Studio ran normally while the device remained online. Each client key must be approved once per phone.
- The user then successfully controlled the Pixel, installed an application, and launched it from Mac Android Studio through the tunnel. Installation and launch took several seconds, confirming functionality while identifying latency as a future measurement and UX concern.
- A future client helper should automate per-device port selection, tunnel monitoring, local connection, authorization guidance, and teardown. Teardown must disconnect local ADB, close the tunnel, and return the phone to USB-only mode. Never copy private ADB keys between clients or expose device port `5555` through router forwarding.
- The client helper is named **PocketPortal Connect**. Keep it as a separate
  executable with a replaceable transport boundary; add a desktop UI, Windows
  packaging, credential-store integration, or alternative transports only
  after the CLI workflow earns that complexity. Do not put desktop lifecycle
  code into the PocketPortal server.
- The first limited-access PocketPortal Connect protocol slice is implemented
  in the independent `VanyaHuaman/PocketPortal-Connect` repository. It creates a loopback-only local
  ADB listener and forwards only ADB bytes through a bearer-authenticated
  WebSocket to one validated server-side device serial. The server bridge is
  disabled by default, requires a 32-character-or-longer environment token,
  never exposes the shared ADB smart socket, and restores USB mode on close.
  The client refuses plaintext WebSockets for non-loopback hosts.
- This bridge has automated application, adapter parsing, configuration,
  WebSocket authorization, and CLI configuration tests. PocketPortal now has a
  configurable Netty HTTPS/WSS connector, separate plaintext and TLS bind
  addresses, a non-destructive Linux certificate/setup script, and client
  support that combines normal JVM trust with an optional PEM bundle. A
  disposable smoke test verified both HTTP and certificate-validated HTTPS.
  The bridge passed end to end against the physical Pixel from personal and
  work Macs, including Android Studio control and application installation,
  corporate-VPN coexistence on the work Mac, and clean restoration to USB mode.
  The raw SSH per-device tunnel remains a diagnostic fallback rather than the
  supported daily workflow.
- The work Mac has employer-managed TLS traffic inspection. The user already
  has a work-provided utility that installs the inspection CA for curl, Node,
  Java, and Ruby. PocketPortal Connect should use the JVM/system trust store by
  default and also accept an explicit PEM CA bundle or Java trust store.
  Certificate pinning can be optional for unmanaged clients but cannot be the
  sole trust mechanism because inspected WSS connections may present the
  employer-issued substitute certificate.
- `scripts/configure-linux-tls.sh --host PRIVATE_LAN_IP` generates an EC
  certificate with an IP SAN, a PKCS12 key store, a random bridge token, and
  owner-only secret files. It keeps HTTP on localhost and binds TLS only to the
  specified LAN address. The script refuses to overwrite existing TLS files.
- Ktor CIO was rejected for the server after an actual HTTPS smoke test showed
  that its server engine does not implement HTTPS. The application now uses
  Ktor Netty; a subsequent smoke test returned ready responses over both HTTP
  and certificate-validated HTTPS.
- The `0.1.7-SNAPSHOT` release was deployed through the health-gated installer.
  TLS is active at `https://192.168.0.151:8443`, while plaintext HTTP remains
  restricted to `127.0.0.1:8080`. The server generated owner-only TLS and
  bridge secrets and remained healthy after restart.
- PocketPortal Connect passed its first real end-to-end non-SSH validation from
  the personal Mac. Local ADB reported `127.0.0.1:15556` as an online Pixel 4
  XL running Android 13 through authenticated WSS. Ctrl+C closed the tunnel,
  removed the local ADB entry, restored the Pixel to its USB serial on the
  server, and left PocketPortal active.
- PocketPortal Connect's `scripts/connect-macos.sh` provides the first low-friction client launcher.
  It builds Connect when missing, discovers ADB from the Android SDK or `PATH`,
  bootstraps the server certificate and bridge token over SSH on first use,
  stores the token in the macOS login Keychain, discovers online devices from
  the API, accepts readable model identifiers such as `pixel-4-xl`, presents a
  styled arrow-key picker with device details when no device is supplied,
  remembers server settings after first use, and starts the scoped tunnel.
- PocketPortal Connect was extracted on July 30, 2026 after its distinct
  client runtime, security boundary, release cadence, packaging needs, and UI
  had become concrete. This repository owns the server bridge and its API
  contract; `VanyaHuaman/PocketPortal-Connect` owns the client engine, macOS
  launcher, Keychain integration, ADB discovery, terminal UI, and packaging.
  The standalone build and physical Pixel path passed before the duplicate
  client module was removed here.
- The live server currently reports two USB Android devices: Pixel 4 XL serial
  `9B011FFBA00A1L` and Lenovo TB336FU serial `HNY09D8P`.
- Android observations now classify phone and tablet form factors from physical
  display metrics. Explicit serial-to-form-factor configuration supports
  clamshell and book-style foldables, whose folded state cannot be identified
  reliably from basic ADB properties. The API exposes the typed form factor and
  the dashboard renders distinct phone, tablet, clamshell, book-foldable, and
  unknown frames around live screenshots.
- The live `0.1.8-SNAPSHOT` API classifies the attached Pixel 4 XL as `phone`
  and the Lenovo TB336FU as `tablet`; the installer health gate and post-deploy
  inventory check passed on July 30, 2026.
- Device screenshots use contain-fit rather than crop-to-fill, and the portrait
  tablet frame follows the attached Lenovo's 10:16 display ratio. This keeps
  system bars and other screen-edge content visible.
- Off-LAN connectivity and coexistence with other VPNs are explicitly the final roadmap decision. Do not integrate Tailscale, WireGuard, Cloudflare Access, a relay, or public SSH yet. Finish the trusted home-network device lab and client workflow first, and never expose ADB or PocketPortal through router port forwarding as a shortcut.
- Frontend dependencies are pinned in `frontend/package-lock.json`; Gradle builds and tests the frontend as part of the normal verification path. The clean-room image uses a dedicated Node build stage and copies only the compiled assets into the JVM build.
- The selected USB hub has been purchased.
- Six Anker USB-A-to-USB-C data cables have been purchased as three two-packs.
- The hardware has not yet been connected, inventoried, or acceptance-tested.

## Committed technical direction

- Architecture: clean architecture with dependencies pointing inward
- Structure: pragmatic modular monolith; use modules where boundaries improve testability, isolation, or future replacement
- Scope control: keep the PocketPortal core lean and extract capabilities only for a concrete host, trust, runtime, lifecycle, scaling, or reuse boundary
- Development practice: implement tests alongside every feature and add regression tests for reproducible bugs
- Configuration: no unexplained magic strings or numbers; deployment values belong in validated config and stable identifiers in module-owned constants
- Setup: preserve a one-command bootstrap and design installation, upgrades, diagnostics, and rollback for low operational friction
- Backend: Kotlin with Ktor
- Concurrency: coroutines, `Flow`, and `SharedFlow`
- Frontend: React with TypeScript and Vite
- Styling: Tailwind CSS with selectively used shadcn/ui components
- Database: SQLite with SQLDelight and versioned migrations
- API: REST for commands and queries
- Realtime dashboard updates: Server-Sent Events
- Future browser control: WebSockets only when bidirectional streaming/input is needed
- Android connectivity: ADB
- Android control: scrcpy
- V1 networking: trusted home LAN with host-firewall restriction
- Process supervision: systemd
- TLS/reverse proxy: deferred until V2/off-LAN access
- Deployment: run directly on Linux with user systemd for V1; do not put the hardware-facing service in Docker
- V1 access boundary: one owner on the trusted home LAN; no PocketPortal login database

The Ktor service should serve the compiled React application so V1 deploys as one application service.

Domain and application code must remain independent of Ktor, SQLite, ADB, scrcpy, Tailscale, filesystem APIs, and process execution. External systems belong behind narrow ports with replaceable adapters. Ktor is the composition and transport edge, not the location for business rules.

The initial structure should separate the application composition root, framework-free domain, application use cases and ports, infrastructure adapters, web transport, and frontend. Split further by device, lease, artifact, installation, identity, audit, and test-run capabilities only when the boundary is useful; do not turn V1 into microservices.

Testing is part of the definition of done. Use fast unit tests for domain rules and use cases, contract/integration tests for adapters and SQLDelight, Ktor API tests for authorization and validation, focused React component tests, and a small critical-path browser suite. Put ADB behind a deterministic fake for routine tests and keep real-device tests in an explicit opt-in hardware suite. CI should run all checks that do not require attached hardware.

PocketPortal is the lean V1 control plane for inventory, browser control, safe
device actions, and APK/AAB installation. User authorization, leases, security
audit history, Maestro/Appium jobs, and off-LAN access are V2 concerns.
Platform-specific or risky execution can live in separate projects.

Separate projects communicate through small authenticated and versioned contracts, initially using HTTPS/JSON unless measured needs justify more. Commands need stable job IDs, idempotency, heartbeats, timeouts, cancellation, and contract tests. Workers must not access PocketPortal's SQLite file directly. Do not add a broker, service mesh, generic event bus, or microservice infrastructure speculatively.

Runtime defaults currently live in `config/pocketportal.properties` and are packaged into the application. `POCKETPORTAL_CONFIG` selects an external file. Server and ADB values have targeted environment overrides. New runtime settings must use the same typed and validated configuration boundary. Named constants live with the module that owns their meaning.

The clean-room harness lives in `testing/clean-room`. It is installation-test infrastructure, not the production deployment model. Its settings are in `clean-room.env`, it targets a named Podman connection, and it uses a unique temporary container name. Continue to deploy the hardware-facing service directly on Linux. Disposable Debian- and Fedora-family VM tests are still needed for systemd, udev, USB, ADB, scrcpy, upgrade, and rollback behavior.

The documentation site is public-facing content only. `PocketPortal-Plan.md` and `AI_HANDOFF.md` are not part of MkDocs navigation or output, but they would still be visible as repository source if the GitHub repository is public. Confirm that publication decision before creating a public remote.

## V1 scope

V1 should provide:

- A home-LAN-only single-owner dashboard showing Android device presence,
  model, OS version, battery, charging state, connection type, last-seen time,
  and a low-frequency screenshot
- Safe actions such as refresh, wake, reboot, screenshot, and starting a control session
- Upload, inspection, confirmation, and installation of one APK onto one explicit device
- Clear handling of offline, unauthorized, recovery, and failed-action states
- Existing scrcpy sessions accessed through a secure remote graphical session initially

V1 must not expose arbitrary shell commands, arbitrary ADB commands, Appium endpoints, uploaded test code, or public control services.

## Multi-user model — V2

- Administrator: manages users, devices, settings, leases, and sessions
- Operator: controls permitted devices and runs approved actions
- Viewer: views permitted devices and health data without sending input
- Every action must be associated with an authenticated user.
- Multiple viewers may be supported later, but only one controller may hold the device lease.
- Typed input, clipboard contents, credentials, notifications, and other session contents must not be audited.
- Roles should be server-wide initially. Add namespaces only if real isolation, quota, billing, retention, or separate-administration requirements emerge.

## Application and test model

Applications are independent artifact records, not children of a project. Derive identity from inspected metadata:

- Package name and label
- Version name and code
- Minimum and target SDK
- Artifact format
- Signing-certificate fingerprint
- SHA-256 checksum
- Optional user tags

Approved Maestro and Appium suites should declare compatible package names, platforms, safe entrypoints, pinned trusted revisions, and permitted environment profiles. Never execute arbitrary uploaded test code as the PocketPortal service account.

## Roadmap highlights

- V1: Single-owner home-LAN Android dashboard, browser control, safe actions,
  Android Studio connection guidance/tooling, and APK/AAB installation
- V2: PocketPortal users, roles, capabilities, leases, security audit events,
  Maestro/Appium jobs, test artifacts, and off-LAN access when real demand exists
- Later: Parallel Android test execution and improved browser-native sessions
- iPhone automation: A dedicated Mac mini or similar Mac host running Xcode, Appium/XCUITest, and WebDriverAgent

For AAB handling, use [`VanyaHuaman/aabx`](https://github.com/VanyaHuaman/aabx) as the behavioral reference. Port its essential workflow into a hardened Kotlin `BundleArtifactService`; do not simply run the existing Node CLI:

```text
.aab -> bundletool build-apks -> device-specific .apks -> bundletool install-apks
```

The prior [`VanyaHuaman/mobile-test-farm`](https://github.com/VanyaHuaman/mobile-test-farm) experiment is useful as a lessons-learned reference, but its visual design and complicated setup should not be reproduced.

## iPhone limitation

The Ubuntu host cannot provide Android-like direct iPhone control. Initial iPhone access is indirect:

1. Keep the iPhone near and paired with a compatible personal Mac.
2. Use Apple iPhone Mirroring.
3. Reach that Mac securely through Tailscale and remote desktop.

A Mac mini is a later Apple Bridge idea, especially for automated physical-iPhone testing. It is not required for Android-focused V1.

## Purchased hardware

- Sabrent HB-BU10 powered USB 3.0 hub
  - 10 USB-A data ports
  - 60 W, 12 V/5 A adapter
  - Individual port switches
  - Purchase link: <https://a.co/d/08k0JsQO>
- Six Anker three-foot USB-A-to-USB-C data cables, purchased as three two-packs

The hub still needs a 24–48 hour six-device acceptance test. Verify stable ADB enumeration, charging under load, reconnect behavior after reboot, physical port-switch behavior, and server USB topology with `lsusb -t`.

## Security invariants

- Never expose ADB, scrcpy, Appium, remote desktop, or internal worker services through router port forwarding.
- Bind internal services to localhost where feasible. When enabling the V1 HTTP
  endpoint on the LAN, restrict it to the trusted home subnet with the host
  firewall.
- Treat APKs, AABs, filenames, metadata, and test suites as untrusted input.
- Use fixed argument arrays such as `ProcessBuilder`; never interpolate browser input into shell commands.
- Validate the target device against inventory immediately before acting and
  serialize operations that could conflict. Add capability and lease checks in
  V2.
- Use server-generated temporary paths, upload limits, timeouts, bounded logs, and predictable cleanup.
- Do not store signing secrets, application secrets, PINs, or credentials in source control or audit events.

## Immediate next action

Continue the lean V1 while the full device lab is assembled:

1. Inventory and label all seven devices, cables, and hub ports.
2. Connect the six Android devices to the powered hub.
3. Install current Android platform tools and scrcpy on Ubuntu.
4. Authorize every device for USB debugging.
5. Run the 24–48 hour hub acceptance test and record battery and disconnect behavior.
6. Test individual and simultaneous scrcpy sessions.
7. Select a lightweight remote graphical session.
8. Enable firewall-restricted home-LAN access and verify it from the personal Mac and PC.
9. Confirm company policy before involving the work Mac.
10. Continue browser control and APK/AAB installation based on observed
    workflow and reliability issues; do not begin V2 platform features.

## Guidance for the next AI

- Treat `PocketPortal-Plan.md` as the source of truth and update it when decisions change.
- Keep V1 deliberately narrow while preserving clean boundaries for later automation.
- Do not introduce project/workspace abstractions or speculative `project_id` fields.
- Do not recreate scrcpy's transport in V1.
- Prefer proven external tools behind narrow, typed PocketPortal operations.
- Before scaffolding code, inspect the current directory and confirm the intended repository/build layout.
- Preserve clean-architecture dependency direction and resist shortcuts that place domain logic in routes, database adapters, process wrappers, or UI components.
- Protect the core from bloat. Before adding a substantial subsystem, decide whether it belongs in the core, an internal module, a reusable library, or a separately deployed worker using the documented extraction criteria.
- Keep cross-project contracts smaller and more stable than internal interfaces, and test compatibility on both sides.
- Add tests with each vertical slice; a feature is not complete merely because its happy path works manually.
- Keep operational values out of source constants, keep stable identifiers out of scattered literals, and test configuration validation and overrides.
- Preserve `./scripts/bootstrap.sh` as the single first-run development path; do not require undocumented manual setup.
- Preserve a simple installation and operating experience; setup complexity was a major failure of the previous experiment.
- When completing meaningful work, update this handoff's current status and next action.
