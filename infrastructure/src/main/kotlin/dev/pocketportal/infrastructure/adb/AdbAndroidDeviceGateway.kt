package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.AndroidDeviceGateway
import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class AdbAndroidDeviceGateway internal constructor(
    private val adbPath: String,
    private val timeout: Duration,
    private val commandRunner: CommandRunner = ProcessCommandRunner(),
) : AndroidDeviceGateway {
    constructor(adbPath: String, timeout: Duration) : this(
        adbPath = adbPath,
        timeout = timeout,
        commandRunner = ProcessCommandRunner(),
    )

    override suspend fun discover(): DeviceDiscoveryResult = withContext(Dispatchers.IO) {
        when (
            val execution = commandRunner.run(
                command = listOf(
                    adbPath,
                    AdbConstants.DEVICES_COMMAND,
                    AdbConstants.LONG_LISTING_FLAG,
                ),
                timeout = timeout,
            )
        ) {
            CommandExecution.ToolNotFound ->
                DeviceDiscoveryResult.Unavailable(DeviceDiscoveryFailure.TOOL_NOT_FOUND)

            CommandExecution.TimedOut ->
                DeviceDiscoveryResult.Unavailable(DeviceDiscoveryFailure.TIMED_OUT)

            is CommandExecution.Completed ->
                if (execution.result.exitCode == SUCCESS_EXIT_CODE) {
                    DeviceDiscoveryResult.Available(
                        devices = AdbDeviceParser.parse(execution.result.standardOutput),
                    )
                } else {
                    DeviceDiscoveryResult.Unavailable(DeviceDiscoveryFailure.COMMAND_FAILED)
                }
        }
    }

    private companion object {
        const val SUCCESS_EXIT_CODE = 0
    }
}
