# API

The API is intentionally small while the architecture is established.

## Service status

```http
GET /api/status
```

Example response:

```json
{
  "service": "PocketPortal",
  "state": "ready",
  "observedAtEpochMillis": 123456789
}
```

## Android devices

```http
GET /api/devices
```

Example response:

```json
{
  "devices": [
    {
      "serial": "example-serial",
      "state": "online",
      "model": "Pixel_8",
      "product": "pixel_product",
      "connectionType": "usb",
      "manufacturer": "Google",
      "androidVersion": "14",
      "sdkLevel": 34,
      "batteryPercentage": 82,
      "chargingState": "charging",
      "screenState": "on",
      "observedAtEpochMillis": 123456789
    }
  ]
}
```

Possible states currently include `online`, `offline`, `unauthorized`, `recovery`, `bootloader`, `sideload`, and `unknown`.

PocketPortal enriches online devices with one bounded, read-only ADB observation. Individual values remain nullable when a device or Android release does not expose them. If the detail command fails or times out, the device remains in the response with its discovery state and observation time instead of failing the entire fleet.

If ADB is missing, times out, or exits unsuccessfully, PocketPortal responds with HTTP `503` and a safe typed error. Raw process output is not returned.

```json
{
  "code": "device_discovery_unavailable",
  "detail": "tool_not_found"
}
```

## Android device screenshot

```http
GET /api/devices/{serial}/screenshot
```

For an online device, this returns the current screen as `image/png`. The
`X-PocketPortal-Observed-At` response header contains the capture timestamp in
epoch milliseconds. PocketPortal verifies the serial against the current ADB
inventory, runs a fixed `adb exec-out screencap -p` command, enforces the
configured timeout, and bounds the captured bytes.

Possible failures are:

| Status | Detail |
| --- | --- |
| `400` | The serial is missing or invalid |
| `404` | The serial is not in the current ADB inventory |
| `409` | The device exists but is not online |
| `413` | The PNG exceeds `android.screenshot.maximumBytes` |
| `503` | ADB is missing, timed out, or the capture command failed |

Screenshots contain whatever is visible on the physical device. Keep direct
access bound to localhost during setup. When enabling V1 home-LAN access,
restrict the HTTP port to the trusted home subnet with the host firewall.
PocketPortal-managed login and off-LAN access are deferred to V2.

## Wake Android device

```http
POST /api/devices/{serial}/actions/wake
```

For an online device, PocketPortal responds with `204 No Content` after sending
Android's named `KEYCODE_WAKEUP`. This action wakes the display if needed; it
does not toggle an already-awake display, dismiss the lock screen, or unlock the
device.

PocketPortal revalidates the serial against the current ADB inventory and runs
a fixed argument list under the configured ADB timeout. An unknown device
returns `404`, a known but non-online device returns `409`, and an ADB execution
failure returns `503`.

!!! warning
    The current endpoint has no application login. Keep PocketPortal bound to
    localhost until firewall-restricted home-LAN access is configured, and
    never expose device actions through router port forwarding.
