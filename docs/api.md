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
      "product": "pixel_product"
    }
  ]
}
```

Possible states currently include `online`, `offline`, `unauthorized`, `recovery`, `bootloader`, `sideload`, and `unknown`.

If ADB is missing, times out, or exits unsuccessfully, PocketPortal responds with HTTP `503` and a safe typed error. Raw process output is not returned.

```json
{
  "code": "device_discovery_unavailable",
  "detail": "tool_not_found"
}
```
