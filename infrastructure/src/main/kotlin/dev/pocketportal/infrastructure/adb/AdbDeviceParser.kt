package dev.pocketportal.infrastructure.adb

import dev.pocketportal.domain.device.AndroidDevice
import dev.pocketportal.domain.device.AndroidConnectionType
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.DeviceSerial

internal object AdbDeviceParser {
    fun parse(
        output: String,
        observedAtEpochMillis: Long = AdbConstants.UNKNOWN_OBSERVATION_TIME_EPOCH_MILLIS,
    ): List<AndroidDevice> =
        output.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { it == AdbConstants.DEVICE_LIST_HEADER }
            .mapNotNull { parseLine(it, observedAtEpochMillis) }
            .toList()

    private fun parseLine(line: String, observedAtEpochMillis: Long): AndroidDevice? {
        val columns = line.split(COLUMN_SEPARATOR)
        if (columns.size < MINIMUM_COLUMN_COUNT) {
            return null
        }

        val metadata = columns
            .drop(METADATA_START_COLUMN)
            .mapNotNull(::parseMetadata)
            .toMap()

        return AndroidDevice(
            serial = DeviceSerial(columns[SERIAL_COLUMN]),
            state = parseState(columns[STATE_COLUMN]),
            model = metadata[AdbConstants.MODEL_METADATA_KEY],
            product = metadata[AdbConstants.PRODUCT_METADATA_KEY],
            connectionType = parseConnectionType(
                serial = columns[SERIAL_COLUMN],
                metadata = metadata,
            ),
            details = null,
            observedAtEpochMillis = observedAtEpochMillis,
        )
    }

    private fun parseMetadata(value: String): Pair<String, String>? {
        val separatorIndex = value.indexOf(AdbConstants.METADATA_SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == value.lastIndex) {
            return null
        }

        return value.substring(0, separatorIndex) to
            value.substring(separatorIndex + AFTER_SEPARATOR_OFFSET)
    }

    private fun parseState(value: String): AndroidDeviceState = when (value) {
        AdbConstants.ONLINE_STATE -> AndroidDeviceState.ONLINE
        AdbConstants.OFFLINE_STATE -> AndroidDeviceState.OFFLINE
        AdbConstants.UNAUTHORIZED_STATE -> AndroidDeviceState.UNAUTHORIZED
        AdbConstants.RECOVERY_STATE -> AndroidDeviceState.RECOVERY
        AdbConstants.BOOTLOADER_STATE -> AndroidDeviceState.BOOTLOADER
        AdbConstants.SIDELOAD_STATE -> AndroidDeviceState.SIDELOAD
        else -> AndroidDeviceState.UNKNOWN
    }

    private fun parseConnectionType(
        serial: String,
        metadata: Map<String, String>,
    ): AndroidConnectionType = when {
        AdbConstants.USB_METADATA_KEY in metadata -> AndroidConnectionType.USB
        AdbConstants.WIRELESS_SERIAL_SEPARATOR in serial -> AndroidConnectionType.WIRELESS
        serial.startsWith(AdbConstants.EMULATOR_SERIAL_PREFIX) -> AndroidConnectionType.UNKNOWN
        else -> AndroidConnectionType.USB
    }

    private const val SERIAL_COLUMN = 0
    private const val STATE_COLUMN = 1
    private const val METADATA_START_COLUMN = 2
    private const val MINIMUM_COLUMN_COUNT = 2
    private const val AFTER_SEPARATOR_OFFSET = 1
    private val COLUMN_SEPARATOR = Regex("\\s+")
}
