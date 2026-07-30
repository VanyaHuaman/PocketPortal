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
    const val METADATA_SEPARATOR = ':'
}
