package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial

interface AndroidAdbBridge {
    suspend fun read(destination: ByteArray): Int
    suspend fun write(source: ByteArray)
    suspend fun close()
}

fun interface AndroidAdbBridgeGateway {
    suspend fun open(serial: DeviceSerial): AndroidAdbBridgeResult
}

sealed interface AndroidAdbBridgeResult {
    data class Opened(val bridge: AndroidAdbBridge) : AndroidAdbBridgeResult
    data class Failed(val reason: AndroidAdbBridgeFailure) : AndroidAdbBridgeResult
}

enum class AndroidAdbBridgeFailure {
    DEVICE_NOT_FOUND,
    DEVICE_NOT_ONLINE,
    DEVICE_ADDRESS_UNAVAILABLE,
    TOOL_NOT_FOUND,
    TIMED_OUT,
    COMMAND_FAILED,
    CONNECTION_FAILED,
}

fun interface OpenAndroidAdbBridge {
    suspend operator fun invoke(serial: DeviceSerial): AndroidAdbBridgeResult
}

class OpenAndroidAdbBridgeUseCase(
    private val gateway: AndroidAdbBridgeGateway,
) : OpenAndroidAdbBridge {
    override suspend fun invoke(serial: DeviceSerial): AndroidAdbBridgeResult =
        gateway.open(serial)
}
