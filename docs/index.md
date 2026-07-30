# PocketPortal

PocketPortal is a self-hosted control plane for a physical mobile-device lab. It is being designed to make a collection of real Android devices available for secure remote access, application installation, and approved automated testing.

## Current capabilities

- A clean-architecture Kotlin/Ktor service
- A responsive, read-only React dashboard served by the same application
- Typed Android discovery through ADB
- Safe reporting of online, offline, unauthorized, and other device states
- Automatic device refresh with explicit loading, empty, and recovery states
- Runtime configuration through properties and environment overrides
- Automated unit, adapter, API, and clean-room installation tests

## Direction

PocketPortal will remain application-project agnostic. It manages devices, users, artifacts, approved test suites, leases, runs, and audit events without requiring source-code projects or workspaces.

The initial deployment target is an Ubuntu home server with physical Android devices connected through a powered USB hub. The hardware-facing service runs directly on the host so USB, ADB, and scrcpy remain straightforward to operate.

[Get started](getting-started/index.md){ .md-button .md-button--primary }
[View the API](api.md){ .md-button }

!!! note
    PocketPortal is under active development and is not ready for unattended production use.
