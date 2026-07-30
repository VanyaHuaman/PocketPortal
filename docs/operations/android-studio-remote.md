# Remote Android Studio

PocketPortal can make a Linux-hosted USB device visible to Android Studio on
another computer by keeping Android Studio on its normal local ADB server and
tunneling one device's authenticated network ADB transport through SSH.

## Experiment result

Do not forward the Linux ADB server's smart socket to Android Studio. The SSH
and ADB protocol path works for CLI commands, but the IDE terminated the shared
server daemon during testing.

During the first Mac-to-Linux test, Android Studio's ADB `36.0.2` repeatedly
terminated the Ubuntu-packaged ADB `34.0.5` server. A CLI connection worked
before Android Studio started. The server was then upgraded to an isolated ADB
`36.0.2` and the experiment was repeated. Android Studio still terminated the
remote daemon, proving that matching versions is necessary operational hygiene
but does not make `ADB_SERVER_SOCKET` a dependable Android Studio integration.

The first server now uses an isolated Platform Tools `36.0.2` installation:

```text
~/.local/share/pocketportal/tools/platform-tools-36.0.2/adb
```

The Ubuntu package remains installed as a fallback. PocketPortal's external
configuration selects the isolated executable with `android.adb.path`.

The archive came from Google's versioned Linux download and had this SHA-256:

```text
3afdea91441815ab41254193df0343d92c1b1c0d0237165c3a345c8af8891c31
```

## Proven per-device workflow

On the Linux host, find the device's Wi-Fi address and temporarily enable its
authenticated network ADB transport:

```bash
adb -s DEVICE_SERIAL shell ip route
adb -s DEVICE_SERIAL tcpip 5555
```

On the client, forward an unused localhost port through the Linux host to that
specific device:

```bash
ssh -N -L 127.0.0.1:5556:DEVICE_WIFI_IP:5555 user@pocketportal-host
```

Connect the client's normal local ADB daemon:

```bash
adb connect 127.0.0.1:5556
adb devices -l
```

The phone prompts once for that client computer's ADB public key. After
authorization, launch Android Studio normally. The device appears as a network
device such as `127.0.0.1:5556`; Android Studio continues using its own local
ADB daemon and does not interfere with PocketPortal's server daemon.

Each client computer has a distinct ADB key and must be approved once on each
phone. Do not copy a private ADB key between computers to avoid approvals.

## End the session

Disconnect the client transport, close the SSH tunnel, and return the phone to
USB-only ADB:

```bash
adb disconnect 127.0.0.1:5556
# Stop the SSH tunnel with Ctrl+C.
adb -s DEVICE_SERIAL usb
```

Run the final command on the Linux host using the USB serial. Network ADB is
temporary and commonly ends after a device reboot, but teardown should be
explicit.

!!! warning
    While enabled, the phone listens for authenticated ADB connections on port
    `5555` on its current network. Use this only on a trusted network, retain
    Android's per-client authorization, never port-forward the device from the
    router, and disable network ADB when the development session ends.

## Future client setup

PocketPortal should eventually provide a small client-side setup helper around
the proven per-device transport. It must not automate the failed shared
remote-server approach. A future helper should:

- Check the Android Studio and server ADB versions before opening a tunnel.
- Select a free localhost port without replacing the user's normal ADB server.
- Establish and monitor the SSH or private-network tunnel.
- Keep Android Studio attached to its normal local ADB server.
- Enable, tunnel, reconnect, monitor, and disable one device transport.
- Track a separate local port for each simultaneous device.
- Report disconnects and version mismatches clearly.
- Restore normal local Android Studio behavior when the session ends.
- Avoid storing passwords or exposing ADB beyond localhost.

The first end-to-end validation used a Pixel 4 XL on Android 13, a Linux host,
and Android Studio on macOS.
