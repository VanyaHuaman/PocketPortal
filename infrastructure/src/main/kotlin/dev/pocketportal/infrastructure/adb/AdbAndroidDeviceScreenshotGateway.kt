package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.AndroidDeviceScreenshotGateway
import dev.pocketportal.application.device.DeviceScreenshotFailure
import dev.pocketportal.application.device.DeviceScreenshotResult
import dev.pocketportal.application.status.Clock
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.DeviceSerial
import dev.pocketportal.infrastructure.time.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class AdbAndroidDeviceScreenshotGateway internal constructor(
    private val adbPath: String,
    private val timeout: Duration,
    private val maximumBytes: Long,
    private val commandRunner: CommandRunner,
    private val binaryCommandRunner: BinaryCommandRunner,
    private val clock: Clock,
) : AndroidDeviceScreenshotGateway {
    constructor(adbPath: String, timeout: Duration, maximumBytes: Long) : this(
        adbPath,
        timeout,
        maximumBytes,
        ProcessCommandRunner(),
        BinaryProcessCommandRunner(),
        SystemClock(),
    )

    override suspend fun capture(serial: DeviceSerial): DeviceScreenshotResult =
        withContext(Dispatchers.IO) {
            val listing = commandRunner.run(
                listOf(adbPath, AdbConstants.DEVICES_COMMAND, AdbConstants.LONG_LISTING_FLAG),
                timeout,
            )
            val completedListing = listing as? CommandExecution.Completed
                ?: return@withContext listing.toScreenshotFailure()
            if (completedListing.result.exitCode != SUCCESS_EXIT_CODE) {
                return@withContext unavailable(DeviceScreenshotFailure.COMMAND_FAILED)
            }
            val device = AdbDeviceParser.parse(completedListing.result.standardOutput)
                .firstOrNull { it.serial == serial }
                ?: return@withContext unavailable(DeviceScreenshotFailure.DEVICE_NOT_FOUND)
            if (device.state != AndroidDeviceState.ONLINE) {
                return@withContext unavailable(DeviceScreenshotFailure.DEVICE_NOT_ONLINE)
            }

            when (
                val execution = binaryCommandRunner.run(
                    listOf(
                        adbPath,
                        AdbConstants.DEVICE_SELECTOR_FLAG,
                        serial.value,
                        AdbConstants.EXEC_OUT_COMMAND,
                        AdbConstants.SCREEN_CAPTURE_COMMAND,
                        AdbConstants.PNG_OUTPUT_FLAG,
                    ),
                    timeout,
                    maximumBytes,
                )
            ) {
                is BinaryCommandExecution.Completed ->
                    if (execution.exitCode == SUCCESS_EXIT_CODE && execution.bytes.isNotEmpty()) {
                        DeviceScreenshotResult.Available(execution.bytes, clock.currentEpochMillis())
                    } else {
                        unavailable(DeviceScreenshotFailure.COMMAND_FAILED)
                    }
                BinaryCommandExecution.ToolNotFound ->
                    unavailable(DeviceScreenshotFailure.TOOL_NOT_FOUND)
                BinaryCommandExecution.TimedOut ->
                    unavailable(DeviceScreenshotFailure.TIMED_OUT)
                BinaryCommandExecution.OutputTooLarge ->
                    unavailable(DeviceScreenshotFailure.OUTPUT_TOO_LARGE)
            }
        }

    private fun CommandExecution.toScreenshotFailure(): DeviceScreenshotResult.Unavailable =
        unavailable(
            when (this) {
                CommandExecution.ToolNotFound -> DeviceScreenshotFailure.TOOL_NOT_FOUND
                CommandExecution.TimedOut -> DeviceScreenshotFailure.TIMED_OUT
                is CommandExecution.Completed -> DeviceScreenshotFailure.COMMAND_FAILED
            },
        )

    private fun unavailable(reason: DeviceScreenshotFailure) =
        DeviceScreenshotResult.Unavailable(reason)

    private companion object {
        const val SUCCESS_EXIT_CODE = 0
    }
}
