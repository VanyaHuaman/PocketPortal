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

PocketPortal remains the control plane and source of truth for identity, authorization, device inventory, leases, job intent, and audit history.
