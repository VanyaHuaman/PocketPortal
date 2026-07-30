# Configuration

Default runtime settings are stored in `config/pocketportal.properties` and packaged with the application.

```properties
server.host=0.0.0.0
server.port=8080
server.tls.enabled=false
server.tls.host=127.0.0.1
server.tls.port=8443
server.tls.keyAlias=pocketportal
android.adb.path=adb
android.adb.timeoutMillis=5000
android.screenshot.maximumBytes=8388608
android.bridge.enabled=false
```

Use `POCKETPORTAL_CONFIG` to select an external properties file. Individual settings currently support these overrides:

| Environment variable | Setting |
| --- | --- |
| `POCKETPORTAL_HOST` | Server bind address |
| `POCKETPORTAL_PORT` | Server port |
| `POCKETPORTAL_TLS_ENABLED` | Enables the HTTPS/WSS connector |
| `POCKETPORTAL_TLS_HOST` | HTTPS/WSS bind address |
| `POCKETPORTAL_TLS_PORT` | HTTPS/WSS port |
| `POCKETPORTAL_TLS_KEY_STORE_PATH` | PKCS12 key-store path |
| `POCKETPORTAL_TLS_KEY_ALIAS` | Private-key alias |
| `POCKETPORTAL_TLS_KEY_STORE_PASSWORD` | PKCS12 password |
| `POCKETPORTAL_TLS_PRIVATE_KEY_PASSWORD` | Private-key password |
| `POCKETPORTAL_ADB_PATH` | ADB executable path |
| `POCKETPORTAL_ADB_TIMEOUT_MILLIS` | ADB command timeout |
| `POCKETPORTAL_SCREENSHOT_MAXIMUM_BYTES` | Maximum accepted screenshot size in bytes |
| `POCKETPORTAL_ADB_BRIDGE_ENABLED` | Enables the narrowly scoped ADB bridge |
| `POCKETPORTAL_ADB_BRIDGE_TOKEN` | Bridge bearer token; required when enabled |

PocketPortal validates required values during startup and fails with a targeted
configuration error when a value is missing or invalid. Bridge tokens must
contain at least 32 characters and should be randomly generated. Linux
installations read secrets from
`~/.config/pocketportal/pocketportal.env`, which the installer creates with
owner-only permissions.

TLS can listen on a private LAN address while the existing HTTP connector
remains on `127.0.0.1`. This preserves local health checks without exposing
plaintext HTTP to the LAN.

!!! warning
    Do not place credentials, signing material, device PINs, application
    secrets, or bridge tokens in checked-in property files.
