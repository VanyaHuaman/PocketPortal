package dev.pocketportal.infrastructure.adb

import dev.pocketportal.domain.device.AndroidChargingState
import dev.pocketportal.domain.device.AndroidDeviceDetails
import dev.pocketportal.domain.device.AndroidScreenState

internal object AdbDeviceDetailsParser {
    fun parse(output: String): AndroidDeviceDetails {
        val sections = parseSections(output)
        val batteryValues = parseKeyValues(sections[AdbConstants.BATTERY_MARKER].orEmpty())
        val batteryLevel = batteryValues[AdbConstants.BATTERY_LEVEL_KEY]?.toIntOrNull()
        val batteryScale = batteryValues[AdbConstants.BATTERY_SCALE_KEY]?.toIntOrNull()

        return AndroidDeviceDetails(
            manufacturer = sections.singleValue(AdbConstants.MANUFACTURER_MARKER),
            androidVersion = sections.singleValue(AdbConstants.ANDROID_VERSION_MARKER),
            sdkLevel = sections.singleValue(AdbConstants.SDK_LEVEL_MARKER)?.toIntOrNull(),
            batteryPercentage = calculateBatteryPercentage(batteryLevel, batteryScale),
            chargingState = parseChargingState(batteryValues),
            screenState = parseScreenState(sections[AdbConstants.POWER_MARKER].orEmpty()),
        )
    }

    private fun parseSections(output: String): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        var currentMarker: String? = null

        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed in MARKERS) {
                currentMarker = trimmed
                sections.getOrPut(trimmed, ::mutableListOf)
            } else {
                currentMarker?.let { sections.getValue(it).add(trimmed) }
            }
        }

        return sections
    }

    private fun parseKeyValues(lines: List<String>): Map<String, String> =
        lines.mapNotNull { line ->
            val separator = line.indexOf(AdbConstants.METADATA_SEPARATOR)
            if (separator <= 0) {
                null
            } else {
                line.substring(0, separator).trim() to
                    line.substring(separator + VALUE_OFFSET).trim()
            }
        }.toMap()

    private fun calculateBatteryPercentage(level: Int?, scale: Int?): Int? =
        if (level == null || scale == null || scale <= INVALID_SCALE) {
            null
        } else {
            (level * PERCENTAGE_SCALE / scale).coerceIn(MINIMUM_PERCENTAGE, MAXIMUM_PERCENTAGE)
        }

    private fun parseChargingState(values: Map<String, String>): AndroidChargingState {
        val status = values[AdbConstants.BATTERY_STATUS_KEY]?.toIntOrNull()
        val powered = POWER_KEYS.any { values[it] == AdbConstants.TRUE_VALUE }

        return when {
            status == AdbConstants.BATTERY_FULL_STATUS -> AndroidChargingState.FULL
            status == AdbConstants.BATTERY_CHARGING_STATUS || powered ->
                AndroidChargingState.CHARGING
            status != null -> AndroidChargingState.NOT_CHARGING
            else -> AndroidChargingState.UNKNOWN
        }
    }

    private fun parseScreenState(lines: List<String>): AndroidScreenState {
        val powerText = lines.joinToString("\n")
        return when {
            powerText.contains(AdbConstants.POWER_AWAKE_MARKER) ||
                powerText.contains(AdbConstants.DISPLAY_ON_MARKER) -> AndroidScreenState.ON

            powerText.contains(AdbConstants.POWER_ASLEEP_MARKER) ||
                powerText.contains(AdbConstants.POWER_DOZING_MARKER) ||
                powerText.contains(AdbConstants.DISPLAY_OFF_MARKER) -> AndroidScreenState.OFF

            else -> AndroidScreenState.UNKNOWN
        }
    }

    private fun Map<String, List<String>>.singleValue(marker: String): String? =
        get(marker)
            ?.firstOrNull(String::isNotBlank)

    private const val VALUE_OFFSET = 1
    private const val INVALID_SCALE = 0
    private const val PERCENTAGE_SCALE = 100
    private const val MINIMUM_PERCENTAGE = 0
    private const val MAXIMUM_PERCENTAGE = 100
    private val MARKERS = setOf(
        AdbConstants.MANUFACTURER_MARKER,
        AdbConstants.ANDROID_VERSION_MARKER,
        AdbConstants.SDK_LEVEL_MARKER,
        AdbConstants.BATTERY_MARKER,
        AdbConstants.POWER_MARKER,
    )
    private val POWER_KEYS = setOf(
        AdbConstants.BATTERY_AC_POWERED_KEY,
        AdbConstants.BATTERY_USB_POWERED_KEY,
        AdbConstants.BATTERY_WIRELESS_POWERED_KEY,
        AdbConstants.BATTERY_DOCK_POWERED_KEY,
    )
}
