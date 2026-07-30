# Remote Android Studio

PocketPortal can experimentally make a Linux-hosted USB device visible to
Android Studio on another computer by forwarding the Linux ADB server over SSH.
ADB remains bound to localhost on both hosts; do not expose port `5037` on the
LAN or internet.

## Experiment result

The SSH and ADB protocol path works for the command-line client, but the direct
Android Studio workflow is not currently supported by PocketPortal.

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

## Reproducing the CLI experiment

Forward an unused local port to the server's localhost-only ADB server:

```bash
ssh -N -L 127.0.0.1:5038:127.0.0.1:5037 user@pocketportal-host
```

Verify the tunnel without disturbing the normal local ADB server:

```bash
ADB_SERVER_SOCKET=tcp:127.0.0.1:5038 adb devices -l
```

Do not launch Android Studio with this `ADB_SERVER_SOCKET`. Close the tunnel
when the CLI experiment ends.

!!! warning
    This is a CLI-only diagnostic experiment, not a supported Android Studio
    workflow or the foundation for PocketPortal browser control. Android Studio
    and PocketPortal would share the server's single ADB daemon; the tested IDE
    startup terminated that daemon until the tunnel was closed.

## Future client setup

PocketPortal should eventually provide a small client-side setup helper, but it
must not automate the failed remote-server approach. First evaluate a
local-ADB transport that securely tunnels a single device, or use Android
Studio on the Linux host through remote desktop. A future helper should:

- Check the Android Studio and server ADB versions before opening a tunnel.
- Select a free localhost port without replacing the user's normal ADB server.
- Establish and monitor the SSH or private-network tunnel.
- Keep Android Studio attached to its normal local ADB server.
- Establish only a proven, isolated device transport if one is selected.
- Report disconnects and version mismatches clearly.
- Restore normal local Android Studio behavior when the session ends.
- Avoid storing passwords or exposing ADB beyond localhost.

Until that design is proven, use this procedure only as an ADB CLI diagnostic.
