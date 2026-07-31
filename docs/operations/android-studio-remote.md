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

The workflow has been verified with a Pixel 4 XL running Android 13 from both a
personal Mac and a work Mac. Android Studio displayed and controlled the
device, installed an application, and launched it. The work-Mac test also
remained functional while its corporate VPN was connected. Installation and
launch took several seconds, so the bridge should not be assumed to match
direct-USB performance.

## Why PocketPortal does not share its ADB server

An earlier experiment forwarded the Linux ADB server's smart socket by setting
`ADB_SERVER_SOCKET` on the Mac. CLI commands worked, but Android Studio
terminated the remote daemon—even after both computers used ADB `36.0.2`.

PocketPortal therefore never exposes its shared ADB smart socket to Android
Studio. The supported bridge scopes a session to one validated physical device
and leaves Android Studio attached to its own local ADB daemon.

## Manual SSH fallback

The original per-device SSH tunnel remains useful for diagnosis if
PocketPortal Connect itself is unavailable. It is not the normal workflow.

On the Linux host, find the device's Wi-Fi address and temporarily enable its
authenticated network ADB transport:

```bash
adb -s DEVICE_SERIAL shell ip route
adb -s DEVICE_SERIAL tcpip 5555
```

On the client, forward an unused loopback port through the Linux host and
connect the local ADB daemon:

```bash
ssh -N -L 127.0.0.1:5556:DEVICE_WIFI_IP:5555 user@pocketportal-host
adb connect 127.0.0.1:5556
adb devices -l
```

To end the fallback session:

```bash
adb disconnect 127.0.0.1:5556
# Stop the SSH tunnel with Ctrl+C.
```

Then restore USB mode from the Linux host:

```bash
adb -s DEVICE_SERIAL usb
```

!!! warning
    While network ADB is enabled, the device listens for authenticated ADB
    connections on port `5555` on its current network. Use it only on a trusted
    network, never forward it through the router, and restore USB mode after
    the session.

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
