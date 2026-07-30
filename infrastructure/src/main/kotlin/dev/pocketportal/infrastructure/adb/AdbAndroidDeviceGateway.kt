package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.AndroidDeviceGateway
import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.status.Clock
import dev.pocketportal.domain.device.AndroidDevice
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.infrastructure.time.SystemClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class AdbAndroidDeviceGateway internal constructor(
    private val adbPath: String,
    private val timeout: Duration,
    private val commandRunner: CommandRunner = ProcessCommandRunner(),
    private val clock: Clock = SystemClock(),
) : AndroidDeviceGateway {
    constructor(adbPath: String, timeout: Duration) : this(
        adbPath = adbPath,
        timeout = timeout,
        commandRunner = ProcessCommandRunner(),
        clock = SystemClock(),
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
                    val observedAt = clock.currentEpochMillis()
                    val discoveredDevices = AdbDeviceParser.parse(
                        output = execution.result.standardOutput,
                        observedAtEpochMillis = observedAt,
                    )
                    DeviceDiscoveryResult.Available(
                        devices = enrichOnlineDevices(discoveredDevices),
                    )
                } else {
                    DeviceDiscoveryResult.Unavailable(DeviceDiscoveryFailure.COMMAND_FAILED)
                }
        }
    }

    private suspend fun enrichOnlineDevices(devices: List<AndroidDevice>): List<AndroidDevice> =
        coroutineScope {
            devices.map { device ->
                async {
                    if (device.state == AndroidDeviceState.ONLINE) {
                        enrich(device)
                    } else {
                        device
                    }
                }
            }.awaitAll()
        }

    private fun enrich(device: AndroidDevice): AndroidDevice {
        val execution = commandRunner.run(
            command = listOf(
                adbPath,
                AdbConstants.DEVICE_SELECTOR_FLAG,
                device.serial.value,
                AdbConstants.SHELL_COMMAND,
                AdbConstants.DETAILS_SHELL_SCRIPT,
            ),
            timeout = timeout,
        )
        val completed = execution as? CommandExecution.Completed
            ?: return device
        if (completed.result.exitCode != SUCCESS_EXIT_CODE) {
            return device
        }

        return device.copy(
            details = AdbDeviceDetailsParser.parse(completed.result.standardOutput),
        )
    }

    private companion object {
        const val SUCCESS_EXIT_CODE = 0
    }
}
