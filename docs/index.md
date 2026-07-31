<div class="pp-hero" markdown>

<p class="pp-kicker">Physical device lab</p>

# Your devices,<br>within reach.

PocketPortal is a self-hosted control plane that makes real Android hardware
visible on a private dashboard and available to Android Studio through a
limited, authenticated device bridge.

[Get started](getting-started/index.md){ .md-button .md-button--primary }
[View the API](api.md){ .md-button }
[PocketPortal Connect](https://vanyahuaman.github.io/PocketPortal-Connect/){ .md-button }

</div>

<figure class="pp-product-shot">
  <a href="assets/android-device-dashboard.png">
    <img
      src="assets/android-device-dashboard.png"
      alt="PocketPortal live inventory showing an online Pixel 4 XL with Android version, battery, screen, connection, and observation details"
      width="2602"
      height="1238"
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
    <p>Typed ADB observations report device state, Android version, battery, charging, screen state, connection type, last-seen time, and a bounded low-frequency screenshot.</p>
  </div>
  <div class="pp-capability">
    <h3>Operationally safe</h3>
    <p>Validated configuration, bounded processes, explicit recovery states, a fixed wake action, versioned installation, diagnostics, and rollback.</p>
  </div>
  <div class="pp-capability">
    <h3>Tested at every boundary</h3>
    <p>Unit, component, adapter, API, clean-room, and real Linux host verification keep changes dependable.</p>
  </div>

</div>

## Built for the lab you actually have

PocketPortal will remain application-project agnostic. V1 is a single-owner
personal lab for trusted-home-LAN device access, browser control, and
application installation without requiring source-code projects or workspaces.
The dashboard and Android Studio bridge are working now; browser-native control
and APK/AAB installation remain active V1 work.
Multi-user identity, roles, leases, managed test runs, and off-LAN access are
deliberately deferred.

The initial deployment target is a Linux home server with physical Android devices connected through a powered USB hub. The hardware-facing service runs directly on the host so USB, ADB, and scrcpy remain straightforward to operate. Ubuntu is the first validated distribution, not an application requirement.

!!! note
    PocketPortal is under active development and is not ready for unattended production use.
