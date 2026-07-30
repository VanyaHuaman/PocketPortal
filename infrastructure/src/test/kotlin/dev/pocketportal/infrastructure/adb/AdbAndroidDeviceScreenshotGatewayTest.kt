package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.DeviceScreenshotFailure
import dev.pocketportal.application.device.DeviceScreenshotResult
import dev.pocketportal.application.status.Clock
import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdbAndroidDeviceScreenshotGatewayTest {
    @Test
    fun `captures a bounded PNG for an online device`() = runTest {
        val binaryCommands = mutableListOf<List<String>>()
        val gateway = gateway(
            listing = ONLINE_DEVICE_LISTING,
            binaryRunner = BinaryCommandRunner { command, _, _ ->
                binaryCommands += command
                BinaryCommandExecution.Completed(SUCCESS_EXIT_CODE, PNG_BYTES)
            },
        )

        val result = assertIs<DeviceScreenshotResult.Available>(
            gateway.capture(DeviceSerial(DEVICE_SERIAL)),
        )

        assertContentEquals(PNG_BYTES, result.pngBytes)
        assertEquals(OBSERVED_AT, result.observedAtEpochMillis)
        assertEquals(
            listOf(
                ADB_PATH,
                AdbConstants.DEVICE_SELECTOR_FLAG,
                DEVICE_SERIAL,
                AdbConstants.EXEC_OUT_COMMAND,
                AdbConstants.SCREEN_CAPTURE_COMMAND,
                AdbConstants.PNG_OUTPUT_FLAG,
            ),
            binaryCommands.single(),
        )
    }

    @Test
    fun `does not run screencap for an unknown serial`() = runTest {
        var captureWasAttempted = false
        val gateway = gateway(
            listing = ONLINE_DEVICE_LISTING,
            binaryRunner = BinaryCommandRunner { _, _, _ ->
                captureWasAttempted = true
                BinaryCommandExecution.Completed(SUCCESS_EXIT_CODE, PNG_BYTES)
            },
        )

        val result = assertIs<DeviceScreenshotResult.Unavailable>(
            gateway.capture(DeviceSerial(UNKNOWN_DEVICE_SERIAL)),
        )

        assertEquals(DeviceScreenshotFailure.DEVICE_NOT_FOUND, result.reason)
        assertEquals(false, captureWasAttempted)
    }

    @Test
    fun `reports screenshots that exceed the configured bound`() = runTest {
        val gateway = gateway(
            listing = ONLINE_DEVICE_LISTING,
            binaryRunner = BinaryCommandRunner { _, _, _ ->
                BinaryCommandExecution.OutputTooLarge
            },
        )

        val result = assertIs<DeviceScreenshotResult.Unavailable>(
            gateway.capture(DeviceSerial(DEVICE_SERIAL)),
        )

        assertEquals(DeviceScreenshotFailure.OUTPUT_TOO_LARGE, result.reason)
    }

    private fun gateway(
        listing: String,
        binaryRunner: BinaryCommandRunner,
    ) = AdbAndroidDeviceScreenshotGateway(
        adbPath = ADB_PATH,
        timeout = COMMAND_TIMEOUT,
        maximumBytes = MAXIMUM_BYTES,
        commandRunner = CommandRunner { _, _ ->
            CommandExecution.Completed(
                CommandResult(SUCCESS_EXIT_CODE, listing, EMPTY_ERROR),
            )
        },
        binaryCommandRunner = binaryRunner,
        clock = Clock { OBSERVED_AT },
    )

    private companion object {
        const val ADB_PATH = "test-adb"
        const val DEVICE_SERIAL = "ABC123"
        const val UNKNOWN_DEVICE_SERIAL = "UNKNOWN"
        const val SUCCESS_EXIT_CODE = 0
        const val EMPTY_ERROR = ""
        const val OBSERVED_AT = 1_234L
        const val MAXIMUM_BYTES = 8_388_608L
        val COMMAND_TIMEOUT: Duration = Duration.ofSeconds(1)
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)
        val ONLINE_DEVICE_LISTING = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            $DEVICE_SERIAL device usb:1-1 product:coral model:Pixel_4_XL
        """.trimIndent()
    }
}
