# PocketPortal

> A self-hosted portal for accessing, monitoring, and managing a personal collection of mobile devices.

## 1. Project vision

PocketPortal will provide a central place to access six Android devices and one iPhone from:

- A personal Mac
- A personal Windows PC
- A work Mac, where company policy permits
- Other authorized devices with a modern browser or remote-desktop client

The Ubuntu home server will act as the always-on device host. Android devices will connect primarily over USB. The initial version will use existing, proven tools instead of building a custom video and input protocol.

## 2. Goals

- Reliably view and control all Android devices remotely.
- Remain application-project agnostic: work with any compatible APK, AAB, package name, UI framework, and approved test suite.
- Keep the devices connected, charged, labeled, and ready to use.
- Provide secure access without exposing ADB or control services to the public internet.
- Support Android development and debugging through Android Studio.
- Provide the best practical access path for the iPhone within Apple's restrictions.
- Start with a simple working system and add a custom web interface only where it provides real value.
- Use clean architecture with explicit dependency boundaries.
- Keep features modular where the separation improves testability, ownership, or future replacement, while deploying V1 as a modular monolith.
- Keep the core product lean. Capabilities that require a different host, trust boundary, runtime, release cadence, or scaling model may become separate projects communicating through small versioned contracts.
- Build automated tests alongside each feature instead of postponing testing until the end.
- Avoid unexplained literal values in implementation code. Put deploy-time values in documented configuration and stable protocol or product values in clearly owned named constants.
- Make first-time installation, upgrades, configuration, verification, and recovery straightforward and well documented.
- Support multiple authenticated users with clear roles and exclusive control sessions.
- Leave room for future testing, automation, health monitoring, shared viewing, and reservations.

## 3. Non-goals for the first version

- Reimplementing scrcpy's screen streaming and input protocol.
- Full browser-based control on day one.
- Direct Linux-hosted control of the iPhone.
- Enterprise mobile-device management.
- Public internet exposure of ADB, scrcpy, or Android Studio.
- Scheduled reservations, billing, or arbitrary custom permission policies in the first version.
- Direct `.aab`, `.apks`, split-APK, or iOS `.ipa` installation in the first version.
- An unrestricted artifact repository or public application-distribution service.
- Arbitrary user-uploaded test code execution in the first version.
- Direct public or user-network access to an Appium server.
- Physical iPhone automation from the Ubuntu host.
- Project, workspace, tenant, billing, or per-team administration concepts.
- Assuming a particular application ID, repository layout, programming language, mobile framework, CI provider, or product such as Scribematic.

## 4. Proposed architecture

```text
                   Home network

 Android 1 ──USB──┐
 Android 2 ──USB──┤
 Android 3 ──USB──┤
 Android 4 ──USB──┼── Ubuntu home server
 Android 5 ──USB──┤     ├── ADB
 Android 6 ──USB──┘     ├── scrcpy
                         ├── PocketPortal dashboard
                         ├── optional Android Studio session
                         ├── remote desktop
                         └── Tailscale

 iPhone ──nearby/paired── Personal Mac
                         ├── Apple iPhone Mirroring
                         ├── remote desktop
                         └── Tailscale

 Personal Mac ────┐
 Personal PC ─────┼── Tailscale private network
 Work Mac ────────┘   (only if company policy permits)
```

## 5. Android access strategy

### Everyday access: scrcpy

Scrcpy should be the primary Android viewing and control engine because it is lightweight, fast, cross-platform, and works with physical devices over USB or TCP/IP. It supports keyboard and mouse control, clipboard sharing, recording, audio forwarding on supported Android versions, and turning off the physical display while maintaining a session.

The Android devices should remain connected to the Ubuntu server by USB whenever practical. USB is more reliable than wireless debugging, supplies power, and avoids address and pairing problems.

Initially, scrcpy windows will be accessed through a remote graphical session on the server. PocketPortal can later launch and manage these sessions.

### Development access: Android Studio

Android Studio Device Mirroring is valuable for development, Logcat, debugging, app inspection, and integrated device interaction. It does not replace the central host: Android Studio normally expects devices to be visible to the ADB instance on the computer where it runs.

The dependable approach is to run Android Studio in a graphical session associated with the Ubuntu host and remotely access that session. Scrcpy and Android Studio can coexist because both use ADB.

Running Android Studio independently on every client and attempting to reach the server's ADB instance is possible to experiment with, but it should not be the foundation of PocketPortal. Remote ADB can introduce security, discovery, reconnection, and tooling-compatibility problems.

### Wireless debugging

Wireless debugging can be retained as a fallback for temporarily undocked devices. It should be limited to the trusted home network and not exposed directly to the internet. DHCP reservations may improve reliability, but USB should remain the default connection.

## 6. iPhone access strategy

iPhone access will use a separate path because iOS does not offer an Android-style ADB and scrcpy control mechanism.

The practical solution is:

1. Keep the iPhone close to a compatible personal Mac.
2. Configure Apple's iPhone Mirroring using the same Apple Account.
3. Add the personal Mac to the private Tailscale network.
4. Remotely access the Mac, then use iPhone Mirroring from that session.

This solution depends on Apple's proximity, account, operating-system, and security requirements. It should be treated as an indirect integration rather than a feature hosted by the Ubuntu server.

If the future goal becomes automated iOS application testing, add a dedicated Mac host with Xcode, Appium, and WebDriverAgent as a separate roadmap phase.

## 7. Secure remote access

Tailscale will provide the private network connecting the server and approved client computers.

Security rules:

- Do not port-forward ADB, scrcpy WebSockets, Android Studio, or remote desktop from the home router.
- Bind PocketPortal services to localhost or the Tailscale interface where feasible.
- Require authentication even when a service is reachable only through Tailscale.
- Use Tailscale access policies to restrict which users and devices can reach PocketPortal.
- Give the work Mac access only if personal VPN software and access to home systems comply with employer policy.
- Do not place personal Apple credentials or Android secrets on a work-managed computer.
- Keep the server, ADB tools, scrcpy, remote-desktop software, and dashboard dependencies updated.
- Record security-relevant events without logging device contents, clipboard data, or credentials.

### Multi-user access model

PocketPortal will be multi-user-ready from its first dashboard release, even if the initial deployment has only one administrator.

Initial roles:

- **Administrator:** Manage users, devices, settings, leases, and all sessions.
- **Operator:** Control authorized devices and run approved actions.
- **Viewer:** View authorized device screens and health information without sending input.

Every API request, device action, and interactive session must be associated with an authenticated user. Use Tailscale identity first where practical, avoiding a PocketPortal-managed password database. Generic OpenID Connect can be considered later for broader self-hosted use.

Interactive control will use an exclusive lease:

- Multiple authorized users may view a device when the streaming implementation supports it.
- Only one user may control a device at a time.
- Device cards show who currently controls the device and when the lease began.
- Leases expire after a configurable period of inactivity.
- An administrator may terminate or take over an active lease.
- A user may watch, wait, or request control when another user holds the lease.
- Control transfers and forced takeovers are recorded in the audit history.

Permissions should be capability-based internally, even while the interface exposes only the three initial roles. Candidate capabilities include `device.view`, `device.control`, `device.reboot`, `app.install`, `logs.view`, `shell.open`, and `users.manage`.

Do not log typed input, clipboard contents, device notifications, passwords, or other sensitive session content. The interface must visibly identify the controller and any observers.

### Application-project-agnostic model

PocketPortal will not model source-code projects or workspaces. It is a shared, trusted device-lab platform organized around users, devices, application artifacts, approved test suites, leases, runs, and audit events.

Application identity comes from inspected artifact metadata rather than server configuration:

- Package name
- Application label
- Version name and code
- Minimum and target SDK
- Artifact format
- Signing-certificate fingerprint
- SHA-256 checksum
- User-assigned tags

Approved Maestro and Appium suites declare their compatible package names, platforms, entrypoints, trusted source revisions, and safe environment requirements. At run time, a user selects a device, compatible artifact, approved suite, and optional environment profile.

Roles are server-wide in V1. Authorized users share the same device pool, artifact catalog, approved suite catalog, and permitted test results. Exclusive leases prevent device conflicts. Use tags and package-name filters for organization rather than project containers.

Add optional namespaces only if real requirements later demand data isolation, separate administrators, device pools, secret scopes, quotas, billing, or retention policies. V1 will not add a hidden workspace or speculative `project_id` to every record.

## 8. Hardware layout

Recommended equipment:

- **Selected for V1:** [Sabrent HB-BU10 10-port powered USB 3.0 hub](https://a.co/d/08k0JsQO)
  - 10 USB-A data ports
  - 60W 12V/5A external power adapter
  - Individual physical power switches and status LEDs
  - Advertised Windows, macOS, and Linux compatibility
- Six short, known-good USB data cables, with four hub ports initially left unused
- Ethernet for the Ubuntu server
- A ventilated shelf, rack, or organizer
- Labels that map each physical device to its friendly name and ADB serial
- Optional controllable USB power or smart plugs for recovery
- Optional temperature sensor near the device shelf

The Sabrent hub is the V1 proof-of-concept choice. Its 60W supply is expected to be sufficient for six Android devices with their physical displays mostly off, but the manufacturer does not provide a strong simultaneous per-port charging guarantee. Before considering a second or more expensive hub, test the selected hub with all six devices for at least 24–48 hours.

Hub acceptance checks:

- All six devices appear consistently in `adb devices`.
- Each device maintains or gains battery while scrcpy is active.
- No device experiences repeated USB or ADB disconnects.
- All devices reconnect after an Ubuntu server reboot.
- Each physical port switch cleanly disconnects and reconnects its device.
- Testing establishes whether each switch cuts charging power as well as USB data.
- `lsusb -t` is recorded to document the server's USB topology.

If the hub cannot maintain charge or stable connections, return or repurpose it and evaluate a hub that explicitly guarantees simultaneous BC 1.2 charging. A second powered hub connected through a separate USB controller remains a future reliability option.

Operational considerations:

- Enable manufacturer battery-protection or charging-limit features where available.
- Avoid enclosing devices while charging continuously.
- Check for swollen batteries and excessive heat periodically.
- Configure devices to remain usable after reboot without weakening lock-screen security unnecessarily.
- Keep a record of device PINs in an appropriate password manager, not in PocketPortal's source repository.

## 9. Delivery phases

### Phase 0: Inventory and decisions

- Record each device's manufacturer, model, Android/iOS version, serial number, USB connector, and intended purpose.
- Decide where the phones and powered hubs will physically live.
- Verify that the Ubuntu server has sufficient USB controllers, power, storage, and cooling.
- Choose the remote-desktop technology.
- Confirm whether the work Mac may connect to the personal Tailscale network.

**Exit condition:** Every device is identified, labeled, and assigned a connection method.

### Phase 1: Working Android lab

- Install current Android platform tools on Ubuntu.
- Enable developer options and USB debugging on each Android device.
- Authorize the Ubuntu server on every device.
- Establish persistent device names based on ADB serial numbers.
- Connect the six devices to the Sabrent HB-BU10, leaving the remaining ports unused during initial testing.
- Record starting and ending battery levels during a multi-hour simultaneous load test.
- Run the hub acceptance checks listed in the hardware section.
- Install and test scrcpy with every device individually.
- Test multiple simultaneous devices.
- Configure a lightweight graphical session and remote desktop.
- Install Tailscale on the server and personal client computers.
- Restrict remote-desktop access to the private network.

**Exit condition:** Each personal computer can securely reach the server and interact with every Android device.

### Phase 2: Development environment

- Install Android Studio only if integrated development tooling is needed on the server.
- Verify Device Mirroring, Logcat, debugging, screenshots, and application deployment.
- Document how Android Studio and scrcpy sessions interact with the same ADB devices.
- Define resource limits so Android Studio does not destabilize the server.

**Exit condition:** A remote development session can inspect and debug any attached Android device reliably.

### Phase 3: PocketPortal dashboard MVP

Build a small authenticated web application that provides:

- Authenticated user identity and administrator, operator, or viewer role
- Device name and photograph or model image
- Online, offline, unauthorized, or recovery status
- ADB serial, model, Android version, battery level, and charging state
- Last-seen timestamp
- Current USB or network connection type
- Latest low-frequency screenshot thumbnail
- Buttons for refresh, wake, reboot, screenshot, and start-control-session
- A controlled single-APK upload and installation workflow for operators and administrators
- An exclusive control lease associated with the authenticated user
- Current controller and lease duration on each occupied device
- Automatic expiration and administrator termination of abandoned leases
- An audit event for every security-relevant action
- Clear error messages for unauthorized or disconnected devices

The backend should invoke a narrow allowlist of predefined operations. It should not initially expose an unrestricted web shell or accept arbitrary ADB commands.

**Exit condition:** A browser provides a trustworthy overview of the Android fleet, safely launches common actions, and attributes every action and control lease to an authenticated user.

#### V1 test-app installation

An operator or administrator who holds the target device's control lease may upload and install one `.apk` file. PocketPortal will:

1. Accept the upload into size-limited temporary storage.
2. Calculate and display the file's SHA-256 checksum.
3. Inspect the APK before installation.
4. Display package name, app label, version name, version code, minimum SDK, target SDK, file size, signing-certificate fingerprint, and requested permissions when available.
5. Compare the uploaded package with the version currently installed on the selected device.
6. Require confirmation before replacing an installed application, downgrading, clearing data, or uninstalling.
7. Re-check the user's `app.install` capability and active device lease immediately before execution.
8. Install through a fixed allowlisted ADB invocation.
9. Optionally grant declared runtime permissions or launch the app after a successful installation.
10. Report progress and a device-specific success or failure result.
11. Delete the temporary file according to a short, explicit retention policy.
12. Append safe metadata and the result to the audit history.

The default replace operation preserves existing application data. Clear-data, downgrade, and uninstall actions remain separate and require explicit confirmation. Uploaded filenames and browser-provided package information are never trusted; PocketPortal derives metadata from the APK itself.

V1 supports a single APK on a single leased device per installation job. It does not accept arbitrary installation flags.

### Phase 4: Session integration

Evaluate the experience from Phase 3 before selecting one of these approaches:

1. Continue launching scrcpy inside remote desktop.
2. Integrate a maintained browser-compatible scrcpy transport.
3. Build a small streaming gateway around the current scrcpy protocol.
4. Evaluate DeviceFarmer/STF or a maintained successor.

Selection criteria:

- Current maintenance status
- Authentication and authorization support
- Encryption
- Latency on local and remote connections
- Multi-device stability
- Mobile-browser support
- Compatibility with current Android and scrcpy releases
- Ease of recovery after USB disconnects or server restarts
- Ability to relay one device stream to multiple viewers
- Ability to accept input exclusively from the current lease holder

**Exit condition:** PocketPortal can start, stop, and recover interactive sessions without unsafe public services.

### Phase 5: iPhone bridge

- Configure iPhone Mirroring on the selected personal Mac.
- Configure secure remote desktop to that Mac through Tailscale.
- Add an iPhone card to PocketPortal that links to instructions or the Mac session.
- Clearly distinguish indirect iPhone access from server-hosted Android control.

**Exit condition:** The iPhone has a documented, repeatable remote-access path.

### Phase 6: Reliability and automation

- Add health checks for ADB, USB state, battery, temperature where available, and disk usage.
- Automatically rediscover devices after reconnect or server reboot.
- Add controlled service restarts.
- Add notifications for prolonged disconnection, overheating, or charging problems.
- Back up configuration and device metadata.
- Add optional scheduled screenshots and recordings.
- Establish job timeouts, cancellation, process cleanup, storage quotas, and artifact-retention primitives that later test runners can reuse.

**Exit condition:** Normal disconnects and server restarts require little or no manual repair.

### Phase 7: Multi-user expansion

- Add device groups and grant users access through group membership.
- Add control requests with accept, reject, and transfer workflows.
- Add shared viewing with one controller and clearly identified observers.
- Relay one device stream to multiple viewers instead of starting one device-side stream per viewer.
- Add optional scheduled reservations, limits, grace periods, and idle-release policies.
- Add generic OpenID Connect for installations that do not use Tailscale identity.
- Add notifications for control requests, transfers, lease expiry, and reservation start times.

**Exit condition:** Multiple users can safely share the device fleet without conflicting control, ambiguous ownership, or unnecessary duplicate streams.

### Phase 8: Device-specific AAB installation — V1.5

Use [aabx](https://github.com/VanyaHuaman/aabx) as the reference implementation for the established AAB workflow:

```text
.aab → bundletool build-apks → device-specific .apks → bundletool install-apks
```

PocketPortal will port the small reusable core into a hardened Kotlin `BundleArtifactService` rather than invoking the current Node CLI unchanged. The standalone `aabx` project remains the original CLI and reference implementation.

- Accept one `.aab` upload into a server-generated, size-limited job directory.
- Calculate its SHA-256 checksum and inspect its manifest and package metadata.
- Require the operator or administrator to hold the target device's exclusive lease.
- Pin and record the Bundletool version.
- Build a device-specific APK set using `--connected-device` and the validated `--device-id`.
- Install the resulting `.apks` with the same validated `--device-id`.
- Use a unique temporary directory and output filename for every job.
- Never interpolate uploaded paths, filenames, package names, device serials, or signing values into shell commands.
- Support install-over-existing and separately confirmed fresh-install modes.
- Explain signing-certificate mismatches and other Bundletool failures in user-facing language.
- Record whether Bundletool's debug-signing fallback, a configured PocketPortal test key, or an explicitly authorized signing profile was used.
- Read signing secrets from protected files or a secret provider, never browser parameters, command-line plaintext, logs, or database values.
- Clean up the generated `.apks`, extracted files, password files, and other job data after the configured retention period.
- Record safe artifact, device, signing-mode, timing, outcome, and cleanup metadata in the audit history.

The production service must correct the assumptions appropriate to the original single-user CLI: no shared `app-temp.apks`, no implicit device selection, no shell pipelines for package inspection, and no cleanup that can touch another job.

**Exit condition:** An authorized lease holder can inspect an AAB, generate the correct APK set for one explicitly selected device, install it, understand signing or compatibility failures, and rely on isolated cleanup.

### Phase 9: Advanced app distribution

- Install one APK on multiple selected devices, acquiring leases atomically before starting.
- Report an independent result for every target so one failure does not cancel other installations.
- Add a private artifact library with checksums, package metadata, upload ownership, retention policies, and version history.
- Retain and reuse validated `.aab` and generated `.apks` artifacts according to explicit policy.
- Add split-APK installation.
- Add install profiles such as preserve data, clean install, grant permissions, and launch after install.
- Add application inventory and version matrices across the device fleet.
- Add controlled rollback to a retained artifact.
- Integrate builds or test artifacts from CI only after scoped credentials and artifact trust policies are defined.

**Exit condition:** Authorized users can safely distribute known test artifacts across compatible leased devices and receive complete per-device results.

### Phase 10: Approved Maestro test jobs — V1.5

Maestro will be PocketPortal's first automated test integration because its Linux CLI can target physical Android devices through ADB and its YAML flows require less runner infrastructure than general-purpose test code.

- Administrators register trusted Maestro repositories, revisions, or suite directories.
- Operators with `tests.run` select an authorized device, installed or uploaded APK, approved suite, and approved environment profile.
- PocketPortal acquires an exclusive automation lease before preparing the device.
- The job may install a known APK, optionally clear app state, run an approved Maestro flow, capture failure screenshots, record the screen, and clean up.
- The selected device is passed through a validated fixed device identifier.
- Manual input and conflicting actions are blocked for the duration of the job.
- Jobs run as a restricted Unix service account with time, process, disk, and output limits.
- Test dependencies and Maestro versions are pinned.
- Secrets are referenced from protected server configuration and are never embedded in uploaded flows, logs, or audit events.
- Results include suite and test status, duration, device and OS, application version and checksum, console output, screenshots, recordings, JUnit XML when available, initiating user, and cancellation or timeout reason.
- Administrators may cancel a job; cancellation must terminate child processes, collect available artifacts, and release the device lease.

V1.5 does not execute arbitrary uploaded Maestro workspaces. Approved suites are selected from administrator-registered sources.

**Exit condition:** An operator can run an approved Maestro suite on one leased physical Android device, observe progress, receive artifacts and a clear result, and rely on cleanup and lease release after success, failure, cancellation, or timeout.

### Phase 11: Appium test jobs — V2

- Run Appium and the UiAutomator2 driver on the Ubuntu host or a restricted local worker.
- Keep Appium bound to localhost; users interact through PocketPortal rather than receiving direct Appium network access.
- Keep insecure Appium features disabled by default, including arbitrary ADB shell execution.
- Allow administrators to register trusted test-suite sources with pinned runtime, dependency, driver, and framework versions.
- Run test suites through isolated, resource-limited workers rather than the PocketPortal service account.
- Support approved Java, Kotlin, JavaScript, Python, or other Appium client suites only when a corresponding hardened runner image or environment exists.
- Reuse PocketPortal installation jobs, exclusive automation leases, progress events, artifact storage, cancellation, timeouts, cleanup, and audit services.
- Store JUnit-compatible results, framework logs, failure screenshots, optional recordings, and safe Appium server logs.

**Exit condition:** A trusted Appium suite can execute against one leased Android device without exposing the Appium server, granting arbitrary host access, or leaving the device or worker in an occupied state.

### Phase 12: Parallel Android test lab — V2.5

- Acquire leases atomically for all requested devices before a coordinated run.
- Run independent suites or shard a suite across selected compatible devices.
- Preserve per-device results so one target failure does not cancel unrelated targets unless the suite policy requests fail-fast behavior.
- Add queues, priorities, concurrency limits, retries, and cancellation policies.
- Display a device-by-test matrix with app version, OS version, status, duration, and artifacts.
- Prevent manual control, app replacement, reboot, or another test job from conflicting with an active automation lease.

**Exit condition:** PocketPortal can safely execute and report parallel or sharded tests across multiple physical Android devices.

### Phase 13: Physical iPhone automation through Apple Bridge

- Add a dedicated compatible Mac host, likely a Mac mini, near the iPhone.
- Install Xcode, Appium, the XCUITest driver, and WebDriverAgent on that Mac.
- Handle Apple signing, device trust, provisioning, and WebDriverAgent lifecycle on the Mac worker.
- Route approved iOS Appium jobs through the Apple Bridge while keeping its Appium service private.
- Treat the Mac as a separately authenticated worker with its own capabilities, health, job limits, and artifacts.
- Do not plan on Maestro for the physical iPhone unless its official physical-iOS support changes and is validated.

**Exit condition:** An approved Appium/XCUITest suite can run against the physical iPhone through the dedicated Mac worker with repeatable signing, cleanup, and reporting.

## 10. V1 software stack

The following stack is the committed direction for PocketPortal V1:

- **Backend language:** Kotlin
- **Backend framework:** Ktor
- **Concurrency:** Kotlin coroutines, `Flow`, and `SharedFlow`
- **Frontend language:** TypeScript
- **Frontend framework:** React
- **Frontend build tool:** Vite
- **Styling:** Tailwind CSS with selectively adopted shadcn/ui components
- **Database:** SQLite
- **Database access:** SQLDelight with versioned schema migrations
- **Commands:** REST endpoints
- **Realtime status:** Server-Sent Events
- **Future interactive streaming:** WebSockets
- **Private network:** Tailscale
- **Android connectivity:** Android platform tools and ADB
- **Android control:** scrcpy
- **Development tooling:** Android Studio, optional
- **Remote graphical access:** A secure remote-desktop solution restricted to Tailscale
- **Service supervision:** systemd
- **Reverse proxy and TLS:** Caddy or a Tailscale-native HTTPS option
- **Authentication:** Tailscale identity where supported, plus an application session
- **Authorization:** Administrator, operator, and viewer roles backed by capability checks
- **Session concurrency:** Exclusive device-control leases with inactivity expiry
- **Audit:** Append-only security and device-action events

### Runtime layout

V1 will run directly on a Linux host with user systemd. The current home server happens to run Ubuntu:

```text
Linux home server
├── adb
├── scrcpy
├── PocketPortal Ktor service
│   ├── REST API
│   ├── SSE event stream
│   ├── device supervisor
│   ├── action runner
│   ├── lease and session manager
│   ├── audit service
│   ├── SQLite database
│   └── compiled React assets
└── systemd
```

The Ktor application will serve the compiled React frontend, producing one deployable application service. Do not place the hardware-facing V1 service in Docker. Direct host access will make USB permissions, ADB ownership, graphical processes, device reconnects, and recovery easier to operate and diagnose. Containerization can be reconsidered after those behaviors are proven.

### Architecture and modularity

PocketPortal V1 will be a modular monolith following clean-architecture dependency rules:

```text
Frameworks and external tools
            ↓
Infrastructure adapters
            ↓
Application use cases
            ↓
Domain model
```

Dependencies point inward. The domain and application layers must not depend on Ktor, SQLite, ADB, scrcpy, Tailscale, the filesystem, or operating-system process APIs. Those integrations sit behind narrow interfaces and are supplied by infrastructure modules.

Prefer modules aligned with stable product capabilities, such as devices, leases, artifacts, installations, identity, audit, and later test runs. Each module owns its use cases and domain rules and exposes a small public contract. Avoid both a single undifferentiated application module and speculative microservices. Features may begin in one Gradle module when small, then move behind a dedicated module boundary when the separation provides meaningful testability, isolation, or reuse.

A likely initial Gradle layout is:

```text
pocketportal/
├── app/                 # Ktor composition root and packaged application
├── domain/              # Framework-free entities, value objects, and policies
├── application/         # Use cases and ports
├── infrastructure/      # SQLite, ADB, processes, filesystem, identity adapters
├── web/                 # Ktor routes, request/response models, SSE
└── frontend/            # React application
```

As capability boundaries become substantial, split `application` and `infrastructure` by feature without changing the one-service deployment model. The application composition root is the only place that should wire concrete adapters to ports.

### Core versus separate projects

Do not let PocketPortal become a container for every device-lab concern. The default is to keep a capability in the modular monolith while it shares the same runtime, trust boundary, data ownership, and release lifecycle. Extract it only when the separation solves a concrete problem.

Strong reasons to create a separate project include:

- It must run on another operating system or physical host.
- It executes untrusted or substantially higher-risk code.
- It has heavyweight or conflicting runtime dependencies.
- It must restart, deploy, scale, or fail independently.
- It has a stable responsibility that can be expressed through a small contract.
- It is useful independently of PocketPortal or is likely to have multiple consumers.

Poor reasons include reducing file count, anticipating hypothetical scale, or making an early diagram look more service-oriented.

Likely extraction candidates are:

- **Apple Bridge:** Runs on a Mac and owns Xcode, WebDriverAgent, and physical-iPhone automation.
- **Test workers:** Run approved Maestro or Appium jobs within a restricted execution boundary.
- **Streaming gateway:** Becomes separate only if browser-native streaming introduces a distinct runtime, security boundary, or scaling need.
- **Artifact tooling:** May become a reusable library or service if AAB/APK processing gains consumers outside PocketPortal; V1 should keep the hardened adapter local.

PocketPortal remains the control plane and source of truth for identity, authorization, device inventory, leases, job intent, and audit history. Separate workers own only their local execution state and return status plus artifacts.

Communication should start simple:

- Define explicit, versioned request and response schemas.
- Use authenticated HTTPS/JSON for low-volume commands and status unless a measured need justifies another protocol.
- Give every command a stable job ID and idempotency behavior.
- Use heartbeats, timeouts, cancellation, bounded retries, and clear compatibility negotiation.
- Do not share PocketPortal's SQLite file or reach directly into another project's database.
- Do not introduce a message broker, service mesh, or generic event bus until a real reliability or throughput requirement demands one.
- Keep contract tests on both sides so independently developed projects remain compatible.

### Test strategy

Testing is part of each feature's definition of done:

- Domain rules and application use cases receive fast unit tests without Ktor, SQLite, ADB, or physical devices.
- Infrastructure adapters receive contract and integration tests using controlled fixtures, temporary directories, fake processes, or disposable databases.
- SQLDelight migrations and constraints receive database tests, including the invariant that a device cannot have more than one active lease.
- Ktor routes receive API tests covering authentication, authorization, validation, error mapping, and safe response data.
- React components and feature flows receive focused component tests; a small browser end-to-end suite covers only critical user journeys.
- Hardware-facing behavior uses an `AdbGateway` or equivalent port so most tests run with a deterministic fake. A separately marked hardware test suite may target real attached devices.
- Every bug fix should include a regression test when the behavior can be reproduced deterministically.

CI should run formatting, static checks, unit tests, integration tests that do not require hardware, frontend tests, and production builds. Hardware tests remain explicit and opt-in until a dedicated lab runner is reliable.

### Configuration and setup

Operational settings such as bind addresses, ports, paths, upload limits, lease durations, process timeouts, retention periods, and tool locations must come from typed, validated configuration. Checked-in configuration supplies safe development defaults; environment variables or an explicitly selected external configuration file may override deployment values.

Stable product names, API paths, capability identifiers, event names, and protocol values belong in constants owned by the module that defines them. Do not scatter magic strings or magic numbers through routes, use cases, adapters, tests, or scripts. Constants are not a substitute for deployment configuration, and configuration is not a substitute for domain value types.

Setup goals:

- A new development checkout uses one documented bootstrap command.
- The repository pins build-tool and dependency versions.
- Bootstrap performs prerequisite checks and runs a representative build and test suite.
- Linux installation will use one documented, distribution-aware workflow with repeatable user-systemd and configuration setup.
- Startup errors identify the missing or invalid setting and the corrective action.
- Upgrade and rollback procedures preserve configuration and database state.
- A diagnostic command will eventually verify Java, ADB, scrcpy, USB permissions, writable paths, database migrations, and network bindings without changing device state.

### API communication

Use REST for commands and state queries:

```text
GET    /api/devices
GET    /api/devices/{id}
POST   /api/devices/{id}/wake
POST   /api/devices/{id}/reboot
POST   /api/devices/{id}/screenshots
POST   /api/devices/{id}/leases
DELETE /api/devices/{id}/leases/current
POST   /api/artifacts/inspect
POST   /api/devices/{id}/installations
GET    /api/installations/{installationId}
```

Use one-way Server-Sent Events for V1 dashboard updates such as device connections, disconnections, battery changes, action results, and lease changes. Introduce WebSockets later when browser-native control requires bidirectional touch, keyboard, clipboard, heartbeat, or stream-signaling messages.

### Backend boundaries

Organize the backend around clean-architecture layers, explicit use cases, ports, and adapters. The following capability-oriented packages may initially live inside the broader Gradle modules and later become modules when their boundaries justify it:

```text
backend/src/main/kotlin/pocketportal/
├── api/
├── auth/
├── devices/
│   ├── DeviceProvider.kt
│   ├── AndroidAdbProvider.kt
│   └── DeviceSupervisor.kt
├── actions/
├── artifacts/
│   ├── ApkArtifactService.kt
│   ├── BundleArtifactService.kt
│   └── SigningProfileProvider.kt
├── leases/
├── sessions/
├── audit/
├── database/
└── Application.kt
```

Browser input must never be interpolated into shell command strings. Each API action maps to a predefined operation, validates the device against PocketPortal's inventory, checks the user's capability and active lease, and invokes tools through fixed argument lists such as Java's `ProcessBuilder`. V1 will not expose a generic ADB-command endpoint or unrestricted shell.

Ktor routes must contain transport concerns only. They parse and validate requests, call application use cases, and map results to HTTP responses. Domain rules do not belong in routes, SQLDelight queries, React components, or process adapters.

APK inspection should use a pinned Android build-tools implementation such as `apkanalyzer` or `aapt2`. Installation uses a fixed operation equivalent to `adb -s <validated-serial> install -r <server-generated-temp-path>`, with any additional supported behavior represented by explicit typed options rather than browser-supplied command flags.

The V1.5 `BundleArtifactService` ports the workflow proven by `aabx` into Kotlin. It invokes a pinned Bundletool through fixed `ProcessBuilder` arguments, always includes a validated device ID when connected devices affect output or installation, uses a unique job directory, captures bounded structured output, enforces timeouts, and cleans only paths owned by that job.

### Frontend boundaries

Organize the React application by product feature:

```text
frontend/src/
├── components/
├── features/
│   ├── devices/
│   ├── sessions/
│   ├── users/
│   └── audit/
├── api/
└── App.tsx
```

The frontend renders only capabilities returned for the current user and device. It must not infer authorization from hidden buttons; the backend remains authoritative.

### Future test-runner boundary

Automation runners are untrusted execution environments attached to a narrowly selected device:

```text
PocketPortal job service
        │
        ├── exclusive automation lease
        ├── approved suite and environment profile
        └── bounded job specification
                    │
                    ▼
          Restricted test worker
             ├── Maestro CLI
             └── Appium + UiAutomator2
                    │
                    ▼
          One validated Android device
```

The PocketPortal service must not execute arbitrary uploaded test code with its own account. Workers receive only the selected job, device, approved artifacts, scoped secrets, and output directory. Appium remains bound to localhost or a private worker network and is never exposed through the home router.

## 11. Dashboard concept

```text
┌──────────────────────────────────────────────────────────────┐
│ PocketPortal                         6 online · 1 indirect    │
├──────────────────────────────────────────────────────────────┤
│ [Pixel 8]       [Galaxy S22]      [Test Device 03]           │
│  Screenshot      Screenshot        Screenshot                │
│  USB · 82%       USB · 74%         Offline                   │
│  [Control]       [Control]         [Diagnose]                │
│  [Install APK]   [Screenshot]      Last seen: 14:32          │
├──────────────────────────────────────────────────────────────┤
│ [iPhone]                                                     │
│  Available through Personal Mac                              │
│  [Open Mac access instructions]                              │
└──────────────────────────────────────────────────────────────┘
```

## 12. Data model for the dashboard

Each device record may include:

- Stable internal ID
- Friendly name
- ADB serial or iPhone identifier
- Platform
- Manufacturer and model
- OS version
- Physical USB port or hub location
- Intended purpose
- Connection state
- Battery and charging state
- Last-seen time
- Preferred control method
- Tags such as `daily`, `testing`, `legacy`, or `automation`
- Notes that do not contain passwords or sensitive personal information

Application artifacts should remain independent records rather than children of a project or workspace. Organize them by inspected package metadata, uploader, timestamps, and optional tags.

Multi-user records should include:

- Users and external identity-provider identifiers
- Roles and capability mappings
- Device groups and user access to those groups
- Interactive sessions and session participants
- Exclusive control leases, activity timestamps, and expiry
- Control requests and transfers
- Future reservations
- Append-only audit events containing user, device, action, timestamp, result, and safe metadata

Application installation records should include:

- Server-generated artifact ID
- Original display filename, normalized for presentation
- SHA-256 checksum and file size
- Inspected package and version metadata
- Signing-certificate fingerprint
- Uploading user and upload timestamp
- Target device and initiating lease
- Explicit installation options
- Artifact format: APK, AAB, APK set, or future split-APK set
- Pinned inspection and Bundletool versions
- Validated target device serial and optional device-spec checksum
- Generated APK-set path, checksum, and retention status
- Signing mode and signing-certificate fingerprint without secret material
- Start time, completion time, status, and safe error summary
- Temporary-file retention and deletion timestamp

Automation records should include:

- Test job ID, framework, pinned framework version, and job status
- Registered suite source, trusted revision, suite name, and safe parameters
- Initiating user, target device, and exclusive automation lease
- Application artifact ID, package, version, and checksum
- Approved environment profile and references to scoped secrets without secret values
- Queue, start, heartbeat, completion, cancellation, and timeout timestamps
- Per-test outcomes and safe error summaries
- JUnit report, screenshots, recordings, console output, and other artifact references
- Worker identity and cleanup result
- Parent run and shard index for future parallel jobs

Approved test-suite records should include:

- Framework and safe entrypoint
- Trusted repository or administrator-registered source
- Pinned revision and dependency/runtime profile
- Compatible platforms and package names
- User-facing name, description, and tags
- Allowed environment profiles and secret references
- Required artifact formats or installation behavior

SQLite is sufficient for the expected home-lab concurrency. The schema should still enforce that a device has no more than one active control lease.

## 13. Important technical risks

### USB reliability

Six devices can expose weak hubs, cables, insufficient power, USB-controller limits, and reconnect issues. Test incrementally and record which physical port each device uses.

### ADB authorization

Devices may become unauthorized after resets, user changes, or debugging-key changes. PocketPortal should detect and clearly report this state instead of repeatedly retrying.

### Remote graphical performance

Running scrcpy through remote desktop means compressing an already encoded device image. Start with conservative frame rate and resolution settings, then tune for the network.

### Browser streaming security

Many scrcpy web experiments are prototypes and may lack authentication, authorization, or encryption. Do not deploy one merely because it works on the LAN.

### Multi-user concurrency and privacy

Two users must never inject input into the same device accidentally. Lease acquisition and release must be atomic, abandoned sessions must expire, and every control path must re-check authorization. Shared viewers must be visible to the controller, and sensitive input, clipboard data, notifications, and session content must not enter the audit log.

### Untrusted APK and AAB uploads

Treat every uploaded APK or AAB as untrusted binary input. Enforce upload-size and concurrency limits, use server-generated job directories, never execute filenames, inspect with pinned tooling, and delete temporary files predictably. Only users with `app.install` and a current device lease may initiate installation. AAB jobs must target an explicit validated device and must not share generated APK-set paths. Audit metadata and outcomes without retaining signing secrets, application secrets, or unrestricted tool output.

### Untrusted test execution

Appium suites are executable programs, and Maestro flows can invoke scripting features. Never run arbitrary uploaded suites as the PocketPortal account. Start with administrator-registered trusted sources, pinned revisions and dependencies, a restricted worker identity, explicit environment-variable allowlists, scoped secret references, timeouts, resource limits, output limits, and reliable process-tree termination. Keep Appium private and leave insecure features disabled unless a specific trusted suite has a reviewed need.

### iPhone limitations

Apple can change mirroring requirements, and managed-device restrictions can disable the feature. Keep the iPhone bridge loosely coupled to the main application.

### Work-device policy

Technical feasibility does not imply authorization. Treat the work Mac as optional until policy is confirmed.

## 14. Initial success criteria

PocketPortal's first useful release is successful when:

- All six Android devices remain connected for several days without systematic failures.
- Every Android device can be viewed and controlled from the personal Mac and PC.
- Access works remotely through the private network without public port forwarding.
- A server restart restores device discovery automatically.
- The dashboard accurately reports device state and does not expose arbitrary command execution.
- Every dashboard request and action is associated with an authenticated user.
- Only one user can control a device at a time, with automatic release of abandoned leases.
- Administrators can identify and terminate active leases, and relevant actions appear in the audit history.
- An authorized lease holder can inspect and install a single test APK, see its verified metadata, confirm replacement, and receive a clear result.
- Unauthorized users and users without the target device lease cannot install applications.
- Temporary APK uploads are size-limited, use server-generated paths, and are deleted according to policy.
- V1.5 can install an AAB onto one explicitly leased device using an isolated, device-specific APK set and a recorded non-secret signing mode.
- The iPhone access path is documented and works through the selected personal Mac.

## 15. First implementation checklist

- [ ] Inventory and label all seven devices.
- [x] Purchase the Sabrent HB-BU10 10-port 60W powered USB hub.
- [x] Purchase six Anker 310 three-foot USB-A-to-USB-C data cables.
- [ ] Label the six Anker cables after assigning devices and hub ports.
- [ ] Complete the 24–48 hour hub acceptance test with all six Android devices.
- [x] Install Android platform tools on Ubuntu.
- [ ] Authorize all Android devices for USB debugging.
- [x] Implement the first clean-architecture ADB discovery slice with typed device states, bounded execution, configuration, safe failures, and automated tests.
- [x] Verify live discovery against an attached device and verify missing-ADB behavior in the Podman clean room.
- [x] Deploy the first runtime artifact to the Ubuntu host as an enabled user systemd service bound only to localhost.
- [x] Verify the real Ubuntu service reports the authorized Pixel 4 XL online while refusing LAN connections to the unauthenticated API.
- [x] Add the first read-only React dashboard with live device cards, automatic refresh, and explicit loading, empty, and error states.
- [x] Replace the validated manual runtime procedure with a resumable installer, prerequisite doctor, versioned upgrades, health verification, automatic failure recovery, and explicit rollback.
- [ ] Test all Android devices with scrcpy.
- [ ] Test several simultaneous scrcpy sessions.
- [ ] Verify APK metadata inspection with representative debug and release builds.
- [ ] Verify single-device APK installation, replacement with preserved data, optional launch, failure reporting, and temporary-file cleanup.
- [ ] Verify that viewers and operators without the target lease cannot install an APK.
- [ ] Use the local `aabx` project as a behavioral reference and capture representative AAB success and failure cases.
- [ ] Port the AAB build/install workflow into a Kotlin `BundleArtifactService` using fixed argument lists.
- [ ] Verify explicit multi-device targeting, unique job directories, signing modes, concurrent-job isolation, failure handling, and cleanup.
- [ ] Define the restricted job and artifact model needed before enabling Maestro.
- [ ] Register and run one trusted Maestro smoke suite as the V1.5 automation milestone.
- [ ] Verify test timeout, cancellation, child-process cleanup, artifact collection, and lease release.
- [ ] Add Appium only after the restricted worker boundary is implemented and reviewed.
- [ ] Select and configure a remote graphical session.
- [ ] Install and restrict Tailscale access.
- [ ] Test access from the personal Mac and PC.
- [ ] Check company policy before connecting the work Mac.
- [ ] Configure the iPhone and personal-Mac mirroring path.
- [ ] Run the basic setup long enough to identify recurring problems.
- [ ] Finalize the PocketPortal MVP requirements based on actual use.
- [ ] Establish clean-architecture dependency checks and the initial Gradle module boundaries before implementing product features.
- [ ] Add the test harness, representative unit/integration/API tests, and CI quality gates with the first vertical feature.
- [ ] Record extraction criteria and keep optional worker protocols separate from PocketPortal's internal domain interfaces.
- [ ] Keep runtime values in typed configuration and module-owned constants; add validation tests with each new setting.
- [x] Maintain a one-command development bootstrap and provide a tested, distribution-neutral Linux installer before expanding the lab deployment.
- [x] Add and verify a Podman clean-room test that builds from a fresh Linux image, runs tests, assembles the distribution, starts as a non-root user, loads external configuration, and probes readiness.
- [x] Add Debian- and Fedora-family container checks for installer distribution detection and prerequisite guidance.
- [ ] Add disposable Debian- and Fedora-family VM installation tests for systemd, udev, ADB, scrcpy, upgrades, and rollback.
- [x] Add a strictly validated MkDocs Material documentation site with isolated local setup and a GitHub Pages deployment workflow.
- [x] Create the public GitHub remote, enable GitHub Actions as the Pages source, and verify the deployed documentation URL.

## 16. Decisions to make after the proof of concept

- Does native scrcpy through remote desktop already provide a good enough experience?
- Is Android Studio needed continuously, or only during development sessions?
- Does PocketPortal need live browser control, or is monitoring plus session launching sufficient?
- When should shared viewing be added beyond the initial exclusive control leases?
- Are scheduled reservations needed, or are live leases and control requests sufficient?
- Should device access be assigned only through roles, or also through device groups?
- When should batch APK deployment and the private artifact library be added?
- Which AAB signing modes should the first PocketPortal deployment enable?
- Should generated device-specific APK sets be deleted immediately or retained briefly for repeat installation on the same device configuration?
- Is CI artifact ingestion useful enough to justify managing scoped CI credentials?
- Which repositories and revisions may administrators register as trusted test sources?
- Which environment variables and secret references may test jobs receive?
- Is container isolation sufficient for Appium runners, or should disposable VMs or another worker boundary be used?
- Have real isolation requirements emerged that justify optional namespaces, or do package names and tags remain sufficient?
- Should devices be individually power-cyclable?
- After V1 hardware behavior is proven, would containerizing the web/control plane improve deployment enough to justify splitting it from the host agent?

## 17. Recommended next action

Build Phase 1 before writing the custom dashboard. A week of using ADB, scrcpy, remote desktop, and Tailscale will reveal the real reliability and workflow problems. PocketPortal should then solve those observed problems instead of predicting them.
