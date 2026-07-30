package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial

fun interface AndroidDeviceWakeGateway {
    suspend fun wake(serial: DeviceSerial): DeviceWakeResult
}

sealed interface DeviceWakeResult {
    data object Completed : DeviceWakeResult
    data class Failed(val reason: DeviceWakeFailure) : DeviceWakeResult
}

enum class DeviceWakeFailure {
    DEVICE_NOT_FOUND,
    DEVICE_NOT_ONLINE,
    TOOL_NOT_FOUND,
    TIMED_OUT,
    COMMAND_FAILED,
}

fun interface WakeAndroidDevice {
    suspend operator fun invoke(serial: DeviceSerial): DeviceWakeResult
}

class WakeAndroidDeviceUseCase(
    private val gateway: AndroidDeviceWakeGateway,
) : WakeAndroidDevice {
    override suspend fun invoke(serial: DeviceSerial): DeviceWakeResult = gateway.wake(serial)
}
