package dev.pocketportal.application.device

import dev.pocketportal.domain.device.DeviceSerial
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAndroidAdbBridgeUseCaseTest {
    @Test
    fun `delegates only the selected device serial to the bridge gateway`() = runTest {
        var selectedSerial: DeviceSerial? = null
        val expected = AndroidAdbBridgeResult.Failed(
            AndroidAdbBridgeFailure.DEVICE_NOT_ONLINE,
        )
        val useCase = OpenAndroidAdbBridgeUseCase(
            gateway = AndroidAdbBridgeGateway { serial ->
                selectedSerial = serial
                expected
            },
        )

        val result = useCase(DeviceSerial(DEVICE_SERIAL))

        assertEquals(DeviceSerial(DEVICE_SERIAL), selectedSerial)
        assertEquals(expected, result)
    }

    private companion object {
        const val DEVICE_SERIAL = "ABC123"
    }
}
