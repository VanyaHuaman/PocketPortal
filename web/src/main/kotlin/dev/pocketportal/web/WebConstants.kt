package dev.pocketportal.web

object WebConstants {
    const val STATUS_PATH = "/api/status"
    const val DEVICES_PATH = "/api/devices"
    const val DEVICE_SCREENSHOT_PATH = "/api/devices/{serial}/screenshot"
    const val DEVICE_SERIAL_PARAMETER = "serial"
    const val SCREENSHOT_UNAVAILABLE_CODE = "device_screenshot_unavailable"
    const val OBSERVED_AT_HEADER = "X-PocketPortal-Observed-At"
    const val READY_STATE = "ready"
    const val DEVICE_DISCOVERY_UNAVAILABLE_CODE = "device_discovery_unavailable"
    const val FRONTEND_ROUTE = "/"
    const val FRONTEND_RESOURCE_PACKAGE = "frontend"
    const val FRONTEND_INDEX_FILE = "index.html"
    const val UNKNOWN_VALUE = "unknown"
}
