<div class="pp-hero" markdown>

<p class="pp-kicker">Physical device lab</p>

# Your devices,<br>within reach.

PocketPortal is a self-hosted control plane that makes real Android hardware available for secure remote access, application installation, and approved automated testing.

[Get started](getting-started/index.md){ .md-button .md-button--primary }
[View the API](api.md){ .md-button }

</div>

<figure class="pp-product-shot">
  <a href="assets/android-device-dashboard.png">
    <img
      src="assets/android-device-dashboard.png"
      alt="PocketPortal live inventory showing an online Pixel 4 XL connected over USB and ADB"
      width="2592"
      height="1138"
    >
  </a>
  <figcaption>Live inventory from the first Android device connected to PocketPortal.</figcaption>
</figure>

## Current capabilities

<div class="pp-capability-grid">
  <div class="pp-capability">
    <h3>One cohesive service</h3>
    <p>A clean-architecture Kotlin/Ktor backend serves the responsive React dashboard as a single deployable application.</p>
  </div>
  <div class="pp-capability">
    <h3>Live Android inventory</h3>
    <p>Typed ADB discovery reports online, offline, unauthorized, and other device states with automatic refresh.</p>
  </div>
  <div class="pp-capability">
    <h3>Operationally safe</h3>
    <p>Validated configuration, bounded processes, explicit recovery states, versioned installation, diagnostics, and rollback.</p>
  </div>
  <div class="pp-capability">
    <h3>Tested at every boundary</h3>
    <p>Unit, component, adapter, API, clean-room, and real Ubuntu host verification keep changes dependable.</p>
  </div>

</div>

## Built for the lab you actually have

PocketPortal will remain application-project agnostic. It manages devices, users, artifacts, approved test suites, leases, runs, and audit events without requiring source-code projects or workspaces.

The initial deployment target is an Ubuntu home server with physical Android devices connected through a powered USB hub. The hardware-facing service runs directly on the host so USB, ADB, and scrcpy remain straightforward to operate.

!!! note
    PocketPortal is under active development and is not ready for unattended production use.
