package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.AndroidDeviceWakeGateway
import dev.pocketportal.application.device.DeviceWakeFailure
import dev.pocketportal.application.device.DeviceWakeResult
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class AdbAndroidDeviceWakeGateway internal constructor(
    private val adbPath: String,
    private val timeout: Duration,
    private val commandRunner: CommandRunner,
) : AndroidDeviceWakeGateway {
    constructor(adbPath: String, timeout: Duration) : this(
        adbPath = adbPath,
        timeout = timeout,
        commandRunner = ProcessCommandRunner(),
    )

    override suspend fun wake(serial: DeviceSerial): DeviceWakeResult = withContext(Dispatchers.IO) {
        val listing = commandRunner.run(
            listOf(adbPath, AdbConstants.DEVICES_COMMAND, AdbConstants.LONG_LISTING_FLAG),
            timeout,
        )
        val completedListing = listing as? CommandExecution.Completed
            ?: return@withContext listing.toWakeFailure()
        if (completedListing.result.exitCode != SUCCESS_EXIT_CODE) {
            return@withContext failed(DeviceWakeFailure.COMMAND_FAILED)
        }
        val device = AdbDeviceParser.parse(completedListing.result.standardOutput)
            .firstOrNull { it.serial == serial }
            ?: return@withContext failed(DeviceWakeFailure.DEVICE_NOT_FOUND)
        if (device.state != AndroidDeviceState.ONLINE) {
            return@withContext failed(DeviceWakeFailure.DEVICE_NOT_ONLINE)
        }

        when (
            val execution = commandRunner.run(
                listOf(
                    adbPath,
                    AdbConstants.DEVICE_SELECTOR_FLAG,
                    serial.value,
                    AdbConstants.SHELL_COMMAND,
                    AdbConstants.INPUT_COMMAND,
                    AdbConstants.KEY_EVENT_COMMAND,
                    AdbConstants.WAKE_UP_KEY_CODE,
                ),
                timeout,
            )
        ) {
            is CommandExecution.Completed ->
                if (execution.result.exitCode == SUCCESS_EXIT_CODE) {
                    DeviceWakeResult.Completed
                } else {
                    failed(DeviceWakeFailure.COMMAND_FAILED)
                }
            CommandExecution.ToolNotFound -> failed(DeviceWakeFailure.TOOL_NOT_FOUND)
            CommandExecution.TimedOut -> failed(DeviceWakeFailure.TIMED_OUT)
        }
    }

    private fun CommandExecution.toWakeFailure(): DeviceWakeResult.Failed =
        failed(
            when (this) {
                CommandExecution.ToolNotFound -> DeviceWakeFailure.TOOL_NOT_FOUND
                CommandExecution.TimedOut -> DeviceWakeFailure.TIMED_OUT
                is CommandExecution.Completed -> DeviceWakeFailure.COMMAND_FAILED
            },
        )

    private fun failed(reason: DeviceWakeFailure) = DeviceWakeResult.Failed(reason)

    private companion object {
        const val SUCCESS_EXIT_CODE = 0
    }
}
