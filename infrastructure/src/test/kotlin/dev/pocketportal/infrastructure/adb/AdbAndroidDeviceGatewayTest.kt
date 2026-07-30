package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.status.Clock
import dev.pocketportal.domain.device.AndroidChargingState
import dev.pocketportal.domain.device.AndroidScreenState
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdbAndroidDeviceGatewayTest {
    @Test
    fun `reports a missing adb executable without leaking process errors`() = runTest {
        val gateway = AdbAndroidDeviceGateway(
            adbPath = ADB_PATH,
            timeout = COMMAND_TIMEOUT,
            commandRunner = CommandRunner { _, _ -> CommandExecution.ToolNotFound },
        )

        val result = assertIs<DeviceDiscoveryResult.Unavailable>(gateway.discover())

        assertEquals(DeviceDiscoveryFailure.TOOL_NOT_FOUND, result.reason)
    }

    @Test
    fun `returns an empty fleet from a successful empty adb listing`() = runTest {
        val gateway = AdbAndroidDeviceGateway(
            adbPath = ADB_PATH,
            timeout = COMMAND_TIMEOUT,
            commandRunner = CommandRunner { _, _ ->
                CommandExecution.Completed(
                    CommandResult(
                        exitCode = SUCCESS_EXIT_CODE,
                        standardOutput = AdbConstants.DEVICE_LIST_HEADER,
                        standardError = EMPTY_ERROR,
                    ),
                )
            },
        )

        val result = assertIs<DeviceDiscoveryResult.Available>(gateway.discover())

        assertEquals(emptyList(), result.devices)
    }

    @Test
    fun `enriches online devices with one bounded detail command`() = runTest {
        val commands = mutableListOf<List<String>>()
        val gateway = AdbAndroidDeviceGateway(
            adbPath = ADB_PATH,
            timeout = COMMAND_TIMEOUT,
            commandRunner = CommandRunner { command, _ ->
                commands += command
                completed(
                    if (AdbConstants.DEVICES_COMMAND in command) {
                        DEVICE_LISTING
                    } else {
                        DEVICE_DETAILS
                    },
                )
            },
            clock = Clock { OBSERVED_AT },
        )

        val result = assertIs<DeviceDiscoveryResult.Available>(gateway.discover())
        val device = result.devices.single()

        assertEquals(EXPECTED_COMMAND_COUNT, commands.size)
        assertEquals(OBSERVED_AT, device.observedAtEpochMillis)
        assertEquals("Google", device.details?.manufacturer)
        assertEquals(EXPECTED_BATTERY, device.details?.batteryPercentage)
        assertEquals(AndroidChargingState.CHARGING, device.details?.chargingState)
        assertEquals(AndroidScreenState.OFF, device.details?.screenState)
    }

    @Test
    fun `keeps the discovered device when detail collection times out`() = runTest {
        val gateway = AdbAndroidDeviceGateway(
            adbPath = ADB_PATH,
            timeout = COMMAND_TIMEOUT,
            commandRunner = CommandRunner { command, _ ->
                if (AdbConstants.DEVICES_COMMAND in command) {
                    completed(DEVICE_LISTING)
                } else {
                    CommandExecution.TimedOut
                }
            },
            clock = Clock { OBSERVED_AT },
        )

        val result = assertIs<DeviceDiscoveryResult.Available>(gateway.discover())

        assertEquals(1, result.devices.size)
        assertEquals(null, result.devices.single().details)
        assertEquals(OBSERVED_AT, result.devices.single().observedAtEpochMillis)
    }

    private fun completed(output: String) = CommandExecution.Completed(
        CommandResult(
            exitCode = SUCCESS_EXIT_CODE,
            standardOutput = output,
            standardError = EMPTY_ERROR,
        ),
    )

    private companion object {
        const val ADB_PATH = "test-adb"
        const val SUCCESS_EXIT_CODE = 0
        const val EMPTY_ERROR = ""
        const val COMMAND_TIMEOUT_SECONDS = 1L
        const val DEVICE_SERIAL = "ABC123"
        const val OBSERVED_AT = 1_234L
        const val EXPECTED_BATTERY = 75
        const val EXPECTED_COMMAND_COUNT = 2
        val DEVICE_LISTING = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            $DEVICE_SERIAL device usb:1-1 product:coral model:Pixel_4_XL
        """.trimIndent()
        val DEVICE_DETAILS = """
            ${AdbConstants.MANUFACTURER_MARKER}
            Google
            ${AdbConstants.ANDROID_VERSION_MARKER}
            13
            ${AdbConstants.SDK_LEVEL_MARKER}
            33
            ${AdbConstants.BATTERY_MARKER}
            USB powered: true
            status: 2
            level: 75
            scale: 100
            ${AdbConstants.POWER_MARKER}
            mWakefulness=Asleep
        """.trimIndent()
        val COMMAND_TIMEOUT: Duration = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS)
    }
}
