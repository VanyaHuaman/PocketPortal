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
    val formFactor: AndroidDeviceFormFactor,
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

enum class AndroidDeviceFormFactor {
    PHONE,
    TABLET,
    FOLDABLE_CLAMSHELL,
    FOLDABLE_BOOK,
    UNKNOWN,
}

object AndroidDeviceFormFactorClassifier {
    fun classify(
        characteristics: Set<String>,
        displayWidthPixels: Int?,
        displayHeightPixels: Int?,
        displayDensityDpi: Int?,
    ): AndroidDeviceFormFactor {
        if (FOLDABLE_CHARACTERISTIC in characteristics) {
            return AndroidDeviceFormFactor.UNKNOWN
        }
        if (
            displayWidthPixels == null ||
            displayHeightPixels == null ||
            displayDensityDpi == null ||
            displayWidthPixels <= 0 ||
            displayHeightPixels <= 0 ||
            displayDensityDpi <= 0
        ) {
            return AndroidDeviceFormFactor.UNKNOWN
        }

        val smallestWidthDp =
            minOf(displayWidthPixels, displayHeightPixels).toLong() *
                ANDROID_BASELINE_DENSITY_DPI /
                displayDensityDpi
        return if (smallestWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
            AndroidDeviceFormFactor.TABLET
        } else {
            AndroidDeviceFormFactor.PHONE
        }
    }

    private const val FOLDABLE_CHARACTERISTIC = "foldable"
    private const val ANDROID_BASELINE_DENSITY_DPI = 160L
    private const val TABLET_SMALLEST_WIDTH_DP = 600L
}
