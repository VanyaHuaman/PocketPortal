package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.DeviceWakeFailure
import dev.pocketportal.application.device.DeviceWakeResult
import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdbAndroidDeviceWakeGatewayTest {
    @Test
    fun `sends a fixed wake key event to an online device`() = runTest {
        val commands = mutableListOf<List<String>>()
        val gateway = gateway { command ->
            commands += command
            completed(if (AdbConstants.DEVICES_COMMAND in command) ONLINE_DEVICE_LISTING else "")
        }

        val result = gateway.wake(DeviceSerial(DEVICE_SERIAL))

        assertEquals(DeviceWakeResult.Completed, result)
        assertEquals(EXPECTED_COMMAND_COUNT, commands.size)
        assertEquals(
            listOf(
                ADB_PATH,
                AdbConstants.DEVICE_SELECTOR_FLAG,
                DEVICE_SERIAL,
                AdbConstants.SHELL_COMMAND,
                AdbConstants.INPUT_COMMAND,
                AdbConstants.KEY_EVENT_COMMAND,
                AdbConstants.WAKE_UP_KEY_CODE,
            ),
            commands.last(),
        )
    }

    @Test
    fun `does not send input to an unknown device`() = runTest {
        val commands = mutableListOf<List<String>>()
        val gateway = gateway { command ->
            commands += command
            completed(ONLINE_DEVICE_LISTING)
        }

        val result = assertIs<DeviceWakeResult.Failed>(
            gateway.wake(DeviceSerial(UNKNOWN_DEVICE_SERIAL)),
        )

        assertEquals(DeviceWakeFailure.DEVICE_NOT_FOUND, result.reason)
        assertEquals(1, commands.size)
    }

    @Test
    fun `maps a wake timeout to a typed failure`() = runTest {
        val gateway = gateway { command ->
            if (AdbConstants.DEVICES_COMMAND in command) {
                completed(ONLINE_DEVICE_LISTING)
            } else {
                CommandExecution.TimedOut
            }
        }

        val result = assertIs<DeviceWakeResult.Failed>(
            gateway.wake(DeviceSerial(DEVICE_SERIAL)),
        )

        assertEquals(DeviceWakeFailure.TIMED_OUT, result.reason)
    }

    private fun gateway(run: (List<String>) -> CommandExecution) =
        AdbAndroidDeviceWakeGateway(
            adbPath = ADB_PATH,
            timeout = COMMAND_TIMEOUT,
            commandRunner = CommandRunner { command, _ -> run(command) },
        )

    private fun completed(output: String) = CommandExecution.Completed(
        CommandResult(SUCCESS_EXIT_CODE, output, EMPTY_ERROR),
    )

    private companion object {
        const val ADB_PATH = "test-adb"
        const val DEVICE_SERIAL = "ABC123"
        const val UNKNOWN_DEVICE_SERIAL = "UNKNOWN"
        const val SUCCESS_EXIT_CODE = 0
        const val EMPTY_ERROR = ""
        const val EXPECTED_COMMAND_COUNT = 2
        val COMMAND_TIMEOUT: Duration = Duration.ofSeconds(1)
        val ONLINE_DEVICE_LISTING = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            $DEVICE_SERIAL device usb:1-1 product:coral model:Pixel_4_XL
        """.trimIndent()
    }
}
