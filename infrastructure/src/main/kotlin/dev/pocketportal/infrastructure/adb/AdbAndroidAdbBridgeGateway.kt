package dev.pocketportal.infrastructure.adb

import dev.pocketportal.application.device.AndroidAdbBridge
import dev.pocketportal.application.device.AndroidAdbBridgeFailure
import dev.pocketportal.application.device.AndroidAdbBridgeGateway
import dev.pocketportal.application.device.AndroidAdbBridgeResult
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration

class AdbAndroidAdbBridgeGateway internal constructor(
    private val adbPath: String,
    private val timeout: Duration,
    private val commandRunner: CommandRunner,
    private val socketFactory: (String, Int, Int) -> Socket,
) : AndroidAdbBridgeGateway {
    constructor(adbPath: String, timeout: Duration) : this(
        adbPath = adbPath,
        timeout = timeout,
        commandRunner = ProcessCommandRunner(),
        socketFactory = ::connectSocket,
    )

    override suspend fun open(serial: DeviceSerial): AndroidAdbBridgeResult =
        withContext(Dispatchers.IO) {
            val listing = when (val result = completed(
                commandRunner.run(
                    listOf(adbPath, AdbConstants.DEVICES_COMMAND, AdbConstants.LONG_LISTING_FLAG),
                    timeout,
                ),
            )) {
                is BridgeCommandResult.Completed -> result.value
                is BridgeCommandResult.Failed -> return@withContext failure(result.reason)
            }
            val device = AdbDeviceParser.parse(listing.standardOutput)
                .firstOrNull { it.serial == serial }
                ?: return@withContext failure(AndroidAdbBridgeFailure.DEVICE_NOT_FOUND)
            if (device.state != AndroidDeviceState.ONLINE) {
                return@withContext failure(AndroidAdbBridgeFailure.DEVICE_NOT_ONLINE)
            }

            val route = when (val result = completed(
                commandRunner.run(
                    listOf(
                        adbPath,
                        AdbConstants.DEVICE_SELECTOR_FLAG,
                        serial.value,
                        AdbConstants.SHELL_COMMAND,
                        AdbConstants.IP_COMMAND,
                        AdbConstants.ROUTE_COMMAND,
                    ),
                    timeout,
                ),
            )) {
                is BridgeCommandResult.Completed -> result.value
                is BridgeCommandResult.Failed -> return@withContext failure(result.reason)
            }
            val deviceAddress = parseDeviceAddress(route.standardOutput)
                ?: return@withContext failure(AndroidAdbBridgeFailure.DEVICE_ADDRESS_UNAVAILABLE)

            when (val result = completed(
                commandRunner.run(
                    listOf(
                        adbPath,
                        AdbConstants.DEVICE_SELECTOR_FLAG,
                        serial.value,
                        AdbConstants.TCPIP_COMMAND,
                        AdbConstants.NETWORK_ADB_PORT.toString(),
                    ),
                    timeout,
                ),
            )) {
                is BridgeCommandResult.Completed -> Unit
                is BridgeCommandResult.Failed -> return@withContext failure(result.reason)
            }

            val socket = try {
                socketFactory(
                    deviceAddress,
                    AdbConstants.NETWORK_ADB_PORT,
                    timeout.toMillis().coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                )
            } catch (_: Exception) {
                restoreUsb(serial)
                return@withContext failure(AndroidAdbBridgeFailure.CONNECTION_FAILED)
            }

            AndroidAdbBridgeResult.Opened(
                SocketAndroidAdbBridge(
                    socket = socket,
                    closeAction = { restoreUsb(serial) },
                ),
            )
        }

    private fun completed(execution: CommandExecution): BridgeCommandResult = when (execution) {
        CommandExecution.ToolNotFound ->
            BridgeCommandResult.Failed(AndroidAdbBridgeFailure.TOOL_NOT_FOUND)
        CommandExecution.TimedOut ->
            BridgeCommandResult.Failed(AndroidAdbBridgeFailure.TIMED_OUT)
        is CommandExecution.Completed ->
            if (execution.result.exitCode == SUCCESS_EXIT_CODE) {
                BridgeCommandResult.Completed(execution.result)
            } else {
                BridgeCommandResult.Failed(AndroidAdbBridgeFailure.COMMAND_FAILED)
            }
    }

    private suspend fun restoreUsb(serial: DeviceSerial) {
        commandRunner.run(
            listOf(
                adbPath,
                AdbConstants.DEVICE_SELECTOR_FLAG,
                serial.value,
                AdbConstants.USB_COMMAND,
            ),
            timeout,
        )
    }

    private fun failure(reason: AndroidAdbBridgeFailure) =
        AndroidAdbBridgeResult.Failed(reason)

    companion object {
        private const val SUCCESS_EXIT_CODE = 0

        internal fun parseDeviceAddress(output: String): String? {
            val tokens = output.lineSequence()
                .flatMap { it.trim().split(Regex("\\s+")).asSequence() }
                .toList()
            val markerIndex = tokens.indexOf(AdbConstants.ROUTE_SOURCE_MARKER)
            return tokens.getOrNull(markerIndex + 1)
                ?.takeIf(::isIpv4Address)
        }

        private fun isIpv4Address(value: String): Boolean {
            val octets = value.split('.')
            return octets.size == IPV4_OCTET_COUNT &&
                octets.all { it.toIntOrNull() in IPV4_MINIMUM..IPV4_MAXIMUM }
        }

        private fun connectSocket(host: String, port: Int, timeoutMillis: Int): Socket =
            Socket().apply {
                connect(InetSocketAddress(host, port), timeoutMillis)
                tcpNoDelay = true
            }

        private const val IPV4_OCTET_COUNT = 4
        private const val IPV4_MINIMUM = 0
        private const val IPV4_MAXIMUM = 255
    }
}

private sealed interface BridgeCommandResult {
    data class Completed(val value: CommandResult) : BridgeCommandResult
    data class Failed(val reason: AndroidAdbBridgeFailure) : BridgeCommandResult
}

private class SocketAndroidAdbBridge(
    private val socket: Socket,
    private val closeAction: suspend () -> Unit,
) : AndroidAdbBridge {
    override suspend fun read(destination: ByteArray): Int = withContext(Dispatchers.IO) {
        socket.getInputStream().read(destination)
    }

    override suspend fun write(source: ByteArray): Unit = withContext(Dispatchers.IO) {
        socket.getOutputStream().write(source)
        socket.getOutputStream().flush()
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            runCatching(socket::close)
        }
        closeAction()
    }
}
