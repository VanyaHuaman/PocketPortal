package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAndroidDeviceScreenshotUseCaseTest {
    @Test
    fun `returns the gateway screenshot result unchanged`() = runTest {
        val expected = DeviceScreenshotResult.Available(PNG_BYTES, OBSERVED_AT)
        val serial = DeviceSerial(DEVICE_SERIAL)
        val useCase = GetAndroidDeviceScreenshotUseCase(
            gateway = AndroidDeviceScreenshotGateway { requestedSerial ->
                assertEquals(serial, requestedSerial)
                expected
            },
        )

        assertEquals(expected, useCase(serial))
    }

    private companion object {
        const val DEVICE_SERIAL = "device-serial"
        const val OBSERVED_AT = 123L
        val PNG_BYTES = byteArrayOf(1, 2, 3)
    }
}
