package dev.pocketportal.domain.device

data class AndroidDevice(
    val serial: DeviceSerial,
    val state: AndroidDeviceState,
    val model: String?,
    val product: String?,
)

@JvmInline
value class DeviceSerial(val value: String) {
    init {
        require(value.isNotBlank()) { "Device serial cannot be blank" }
    }
}

enum class AndroidDeviceState {
    ONLINE,
    OFFLINE,
    UNAUTHORIZED,
    RECOVERY,
    BOOTLOADER,
    SIDELOAD,
    UNKNOWN,
}
