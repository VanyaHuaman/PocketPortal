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
