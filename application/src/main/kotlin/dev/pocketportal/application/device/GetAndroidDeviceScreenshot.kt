package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial

fun interface AndroidDeviceScreenshotGateway {
    suspend fun capture(serial: DeviceSerial): DeviceScreenshotResult
}

sealed interface DeviceScreenshotResult {
    data class Available(
        val pngBytes: ByteArray,
        val observedAtEpochMillis: Long,
    ) : DeviceScreenshotResult

    data class Unavailable(val reason: DeviceScreenshotFailure) : DeviceScreenshotResult
}

enum class DeviceScreenshotFailure {
    DEVICE_NOT_FOUND,
    DEVICE_NOT_ONLINE,
    TOOL_NOT_FOUND,
    TIMED_OUT,
    COMMAND_FAILED,
    OUTPUT_TOO_LARGE,
}

fun interface GetAndroidDeviceScreenshot {
    suspend operator fun invoke(serial: DeviceSerial): DeviceScreenshotResult
}

class GetAndroidDeviceScreenshotUseCase(
    private val gateway: AndroidDeviceScreenshotGateway,
) : GetAndroidDeviceScreenshot {
    override suspend fun invoke(serial: DeviceSerial): DeviceScreenshotResult =
        gateway.capture(serial)
}
