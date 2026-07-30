package dev.pocketportal.infrastructure.adb

internal object AdbConstants {
    const val DEVICES_COMMAND = "devices"
    const val LONG_LISTING_FLAG = "-l"
    const val DEVICE_LIST_HEADER = "List of devices attached"
    const val ONLINE_STATE = "device"
    const val OFFLINE_STATE = "offline"
    const val UNAUTHORIZED_STATE = "unauthorized"
    const val RECOVERY_STATE = "recovery"
    const val BOOTLOADER_STATE = "bootloader"
    const val SIDELOAD_STATE = "sideload"
    const val MODEL_METADATA_KEY = "model"
    const val PRODUCT_METADATA_KEY = "product"
    const val USB_METADATA_KEY = "usb"
    const val METADATA_SEPARATOR = ':'
    const val DEVICE_SELECTOR_FLAG = "-s"
    const val SHELL_COMMAND = "shell"

    const val MANUFACTURER_MARKER = "__POCKETPORTAL_MANUFACTURER__"
    const val ANDROID_VERSION_MARKER = "__POCKETPORTAL_ANDROID_VERSION__"
    const val SDK_LEVEL_MARKER = "__POCKETPORTAL_SDK_LEVEL__"
    const val BATTERY_MARKER = "__POCKETPORTAL_BATTERY__"
    const val POWER_MARKER = "__POCKETPORTAL_POWER__"
    const val DETAILS_SHELL_SCRIPT =
        "printf '$MANUFACTURER_MARKER\\n'; getprop ro.product.manufacturer; " +
            "printf '$ANDROID_VERSION_MARKER\\n'; getprop ro.build.version.release; " +
            "printf '$SDK_LEVEL_MARKER\\n'; getprop ro.build.version.sdk; " +
            "printf '$BATTERY_MARKER\\n'; dumpsys battery; " +
            "printf '$POWER_MARKER\\n'; dumpsys power | grep 'mWakefulness='"

    const val BATTERY_LEVEL_KEY = "level"
    const val BATTERY_SCALE_KEY = "scale"
    const val BATTERY_STATUS_KEY = "status"
    const val BATTERY_AC_POWERED_KEY = "AC powered"
    const val BATTERY_USB_POWERED_KEY = "USB powered"
    const val BATTERY_WIRELESS_POWERED_KEY = "Wireless powered"
    const val BATTERY_DOCK_POWERED_KEY = "Dock powered"
    const val BATTERY_CHARGING_STATUS = 2
    const val BATTERY_FULL_STATUS = 5
    const val TRUE_VALUE = "true"
    const val POWER_AWAKE_MARKER = "mWakefulness=Awake"
    const val POWER_ASLEEP_MARKER = "mWakefulness=Asleep"
    const val POWER_DOZING_MARKER = "mWakefulness=Dozing"
    const val DISPLAY_ON_MARKER = "state=ON"
    const val DISPLAY_OFF_MARKER = "state=OFF"
    const val WIRELESS_SERIAL_SEPARATOR = ':'
    const val EMULATOR_SERIAL_PREFIX = "emulator-"
    const val UNKNOWN_OBSERVATION_TIME_EPOCH_MILLIS = 0L
    const val EXEC_OUT_COMMAND = "exec-out"
    const val SCREEN_CAPTURE_COMMAND = "screencap"
    const val PNG_OUTPUT_FLAG = "-p"
    const val INPUT_COMMAND = "input"
    const val KEY_EVENT_COMMAND = "keyevent"
    const val WAKE_UP_KEY_CODE = "KEYCODE_WAKEUP"
}
