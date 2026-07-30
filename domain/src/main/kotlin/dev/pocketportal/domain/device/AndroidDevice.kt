package dev.pocketportal.domain.device

data class AndroidDevice(
    val serial: DeviceSerial,
    val state: AndroidDeviceState,
    val model: String?,
    val product: String?,
    val connectionType: AndroidConnectionType,
    val details: AndroidDeviceDetails?,
    val observedAtEpochMillis: Long,
)

data class AndroidDeviceDetails(
    val manufacturer: String?,
    val androidVersion: String?,
    val sdkLevel: Int?,
    val batteryPercentage: Int?,
    val chargingState: AndroidChargingState,
    val screenState: AndroidScreenState,
) {
    init {
        require(batteryPercentage == null || batteryPercentage in MINIMUM_BATTERY..MAXIMUM_BATTERY) {
            "Battery percentage must be between $MINIMUM_BATTERY and $MAXIMUM_BATTERY"
        }
        require(sdkLevel == null || sdkLevel > MINIMUM_SDK_EXCLUSIVE) {
            "SDK level must be positive"
        }
    }

    private companion object {
        const val MINIMUM_BATTERY = 0
        const val MAXIMUM_BATTERY = 100
        const val MINIMUM_SDK_EXCLUSIVE = 0
    }
}

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

enum class AndroidConnectionType {
    USB,
    WIRELESS,
    UNKNOWN,
}

enum class AndroidChargingState {
    CHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN,
}

enum class AndroidScreenState {
    ON,
    OFF,
    UNKNOWN,
}
