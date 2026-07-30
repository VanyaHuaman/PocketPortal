# Configuration

Default runtime settings are stored in `config/pocketportal.properties` and packaged with the application.

```properties
server.host=0.0.0.0
server.port=8080
android.adb.path=adb
android.adb.timeoutMillis=5000
android.screenshot.maximumBytes=8388608
```

Use `POCKETPORTAL_CONFIG` to select an external properties file. Individual settings currently support these overrides:

| Environment variable | Setting |
| --- | --- |
| `POCKETPORTAL_HOST` | Server bind address |
| `POCKETPORTAL_PORT` | Server port |
| `POCKETPORTAL_ADB_PATH` | ADB executable path |
| `POCKETPORTAL_ADB_TIMEOUT_MILLIS` | ADB command timeout |
| `POCKETPORTAL_SCREENSHOT_MAXIMUM_BYTES` | Maximum accepted screenshot size in bytes |

PocketPortal validates required values during startup and fails with a targeted configuration error when a value is missing or invalid. Screenshot capture uses the same ADB timeout and rejects output larger than the configured positive byte limit.

!!! warning
    Do not place credentials, signing material, device PINs, or application secrets in the checked-in configuration.
