package dev.pocketportal.application.device

import dev.pocketportal.domain.device.AndroidDevice

fun interface AndroidDeviceGateway {
    suspend fun discover(): DeviceDiscoveryResult
}

sealed interface DeviceDiscoveryResult {
    data class Available(val devices: List<AndroidDevice>) : DeviceDiscoveryResult
    data class Unavailable(val reason: DeviceDiscoveryFailure) : DeviceDiscoveryResult
}

enum class DeviceDiscoveryFailure {
    TOOL_NOT_FOUND,
    TIMED_OUT,
    COMMAND_FAILED,
}

fun interface GetAndroidDevices {
    suspend operator fun invoke(): DeviceDiscoveryResult
}

class GetAndroidDevicesUseCase(
    private val gateway: AndroidDeviceGateway,
) : GetAndroidDevices {
    override suspend fun invoke(): DeviceDiscoveryResult = gateway.discover()
}
