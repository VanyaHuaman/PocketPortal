package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WakeAndroidDeviceUseCaseTest {
    @Test
    fun `returns the gateway action result unchanged`() = runTest {
        val serial = DeviceSerial(DEVICE_SERIAL)
        val useCase = WakeAndroidDeviceUseCase(
            gateway = AndroidDeviceWakeGateway { requestedSerial ->
                assertEquals(serial, requestedSerial)
                DeviceWakeResult.Completed
            },
        )

        assertEquals(DeviceWakeResult.Completed, useCase(serial))
    }

    private companion object {
        const val DEVICE_SERIAL = "device-serial"
    }
}
