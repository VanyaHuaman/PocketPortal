package dev.pocketportal.application.device

import dev.pocketportal.domain.device.AndroidDevice
import dev.pocketportal.domain.device.AndroidConnectionType
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAndroidDevicesUseCaseTest {
    @Test
    fun `returns the gateway discovery result unchanged`() = runTest {
        val expected = DeviceDiscoveryResult.Available(
            devices = listOf(
                AndroidDevice(
                    serial = DeviceSerial(DEVICE_SERIAL),
                    state = AndroidDeviceState.ONLINE,
                    model = DEVICE_MODEL,
                    product = DEVICE_PRODUCT,
                    connectionType = AndroidConnectionType.USB,
                    details = null,
                    observedAtEpochMillis = OBSERVED_AT,
                ),
            ),
        )
        val useCase = GetAndroidDevicesUseCase(
            gateway = AndroidDeviceGateway { expected },
        )

        assertEquals(expected, useCase())
    }

    private companion object {
        const val DEVICE_SERIAL = "device-serial"
        const val DEVICE_MODEL = "Pixel"
        const val DEVICE_PRODUCT = "pixel_product"
        const val OBSERVED_AT = 123L
    }
}
