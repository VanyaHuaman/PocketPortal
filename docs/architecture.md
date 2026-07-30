# Architecture

PocketPortal is a modular monolith built with clean-architecture dependency rules.

```text
Frameworks and external tools
            ↓
Infrastructure adapters
            ↓
Application use cases
            ↓
Domain model
```

## Core principles

- Dependencies point inward.
- Runtime values use typed, validated configuration.
- Stable identifiers use constants owned by the defining module.
- Browser input is never interpolated into shell commands.
- External commands use fixed argument lists, timeouts, and safe result mapping.
- Every feature includes automated tests.
- The core remains lean.

## Separate workers

A capability may become a separate project when it has a real host, trust, runtime, lifecycle, scaling, or reuse boundary. Likely future examples include:

- A Mac-based Apple Bridge for physical-iPhone automation
- Restricted Maestro and Appium workers
- A browser-streaming gateway, if streaming develops distinct operational needs

PocketPortal V1 remains the single-owner, home-LAN control plane for device
inventory, browser control, safe actions, and application installation.
Identity, authorization, leases, managed Maestro/Appium runs, security audit
history, and off-LAN access enter only with V2 requirements.

## PocketPortal Connect

The client bridge is a separate executable module. Android Studio keeps its
normal local ADB daemon, which connects to a loopback-only listener. The client
forwards only that connection through an authenticated WebSocket to a
server-side adapter that validates one device serial, temporarily enables the
device's authenticated network ADB transport, and restores USB-only mode when
the session closes.

The bridge is disabled by default. Non-loopback client connections require
TLS. The Netty HTTPS/WSS connector, certificate setup, physical Pixel tunnel,
local ADB connection, and USB restoration have passed end-to-end validation.
