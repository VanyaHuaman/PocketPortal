# Remote Android Studio

PocketPortal Connect makes one Android device attached to the Linux server
available to Android Studio on another computer. Android Studio keeps using its
normal local ADB daemon; Connect opens a loopback-only ADB endpoint and carries
that device's traffic over PocketPortal's authenticated WSS bridge.

[Install PocketPortal Connect](https://vanyahuaman.github.io/PocketPortal-Connect/getting-started/installation/){ .md-button .md-button--primary }

## Supported workflow

The client and its macOS launcher are maintained in the independent
[`PocketPortal-Connect`](https://github.com/VanyaHuaman/PocketPortal-Connect)
repository. The launcher:

1. Finds ADB in the Android SDK or `PATH`.
2. Retrieves the PocketPortal certificate and bridge credential over SSH on
   first use.
3. Stores the bridge credential in the macOS login Keychain.
4. Presents the online devices returned by PocketPortal.
5. Connects the selected device to the Mac's normal local ADB daemon.
6. Removes the local endpoint and asks the server to restore USB mode when the
   session ends.

SSH is used only for first-run bootstrap. The active device session is a
limited, bearer-authenticated WSS connection; it does not provide a Linux
shell, arbitrary port forwarding, or access to the server's shared ADB daemon.

Each client computer has a distinct ADB key. Android may ask the owner to
approve that computer once for each physical device. Do not copy private ADB
keys between computers.

## Security boundary

- Non-loopback PocketPortal connections require `wss://`.
- The local ADB endpoint binds only to `127.0.0.1`.
- The server validates the selected serial against live inventory.
- The bridge credential is stored in Keychain rather than a shell profile or
  repository file.
- Managed Macs may use their normal Java trust store plus an explicit
  PocketPortal PEM certificate.
- Router port forwarding is not a supported deployment method.

See the
[PocketPortal Connect documentation](https://vanyahuaman.github.io/PocketPortal-Connect/)
for current installation, terminal controls, managed-Mac trust, and
troubleshooting instructions.
