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

## PocketPortal Connect

The repository now contains the first `pocketportal-connect` CLI and a
disabled-by-default server bridge. Unlike the SSH proof, the bridge accepts
only binary ADB traffic for one validated device serial. It does not provide a
Linux shell or arbitrary network forwarding.

### Easy macOS launcher

The macOS launcher builds the client when necessary, finds ADB from the Android
SDK or `PATH`, copies the server certificate on first use, retrieves the bridge
token over SSH, and stores that token in the current user's login Keychain:

```bash
./scripts/connect-macos.sh \
  --server wss://192.168.0.151:8443 \
  --ssh-target vanya@192.168.0.151
```

The first run may request SSH and Keychain approval. Subsequent runs reuse the
certificate and Keychain credential. If more than one device is online, the
launcher presents a numbered picker. A model-based identifier is also accepted:

```bash
./scripts/connect-macos.sh \
  --server wss://192.168.0.151:8443 \
  --ssh-target vanya@192.168.0.151 \
  --device pixel-4-xl
```

To shorten the recurring command, set these in the shell profile:

```bash
export POCKETPORTAL_CONNECT_SERVER='wss://192.168.0.151:8443'
export POCKETPORTAL_CONNECT_SSH_TARGET='vanya@192.168.0.151'
```

Then connecting a device requires only:

```bash
./scripts/connect-macos.sh
```

The underlying client listens only on `127.0.0.1`, asks the local ADB daemon
to connect to that port, and carries the bytes over an authenticated WebSocket.
Advanced users may still build and invoke it directly with
`./gradlew :connect:installDist`.

Non-loopback servers must use `wss://`; unencrypted `ws://` is accepted only
for loopback development. The server-side bridge is enabled with
`android.bridge.enabled=true` and reads its token only from
`POCKETPORTAL_ADB_BRIDGE_TOKEN`.

Managed computers may use TLS inspection. The client uses the JVM trust store
by default and combines it with the optional PEM bundle supplied through
`--ca-certificate`. A company CA already installed into Java therefore
continues to work, while the PEM bundle trusts PocketPortal's private
host-specific certificate when traffic is not intercepted.

!!! warning
    TLS support, setup tooling, local ADB connectivity, and clean USB
    restoration have passed their first live Pixel validation. Do not expose
    the plaintext HTTP connector or bridge through router port forwarding.

The implementation preserves these requirements:

- Check the Android Studio and server ADB versions before opening a tunnel.
- Select a free localhost port without replacing the user's normal ADB server.
- Establish and monitor the scoped PocketPortal tunnel.
- Keep Android Studio attached to its normal local ADB server.
- Enable, tunnel, reconnect, monitor, and disable one device transport.
- Track a separate local port for each simultaneous device.
- Report disconnects and version mismatches clearly.
- Restore normal local Android Studio behavior when the session ends.
- Avoid storing passwords or exposing ADB beyond localhost.

The first end-to-end validation used a Pixel 4 XL on Android 13, a Linux host,
and Android Studio on macOS. The user successfully interacted with the device,
installed an application, and launched it from Android Studio. Installation and
launch took several seconds, so this workflow should be treated as functional
but not assumed to match direct-USB performance.
