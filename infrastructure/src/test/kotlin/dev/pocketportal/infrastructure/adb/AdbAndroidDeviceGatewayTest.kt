package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
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

    private companion object {
        const val ADB_PATH = "test-adb"
        const val SUCCESS_EXIT_CODE = 0
        const val EMPTY_ERROR = ""
        const val COMMAND_TIMEOUT_SECONDS = 1L
        val COMMAND_TIMEOUT: Duration = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS)
    }
}
