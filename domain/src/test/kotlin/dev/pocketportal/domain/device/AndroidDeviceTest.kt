package dev.pocketportal.domain.device

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AndroidDeviceTest {
    @Test
    fun `rejects a blank device serial`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceSerial(BLANK_SERIAL)
        }
    }

    @Test
    fun `rejects a battery percentage outside the physical range`() {
        assertFailsWith<IllegalArgumentException> {
            AndroidDeviceDetails(
                manufacturer = null,
                androidVersion = null,
                sdkLevel = null,
                batteryPercentage = INVALID_BATTERY_PERCENTAGE,
                chargingState = AndroidChargingState.UNKNOWN,
                screenState = AndroidScreenState.UNKNOWN,
                formFactor = AndroidDeviceFormFactor.UNKNOWN,
            )
        }
    }

    private companion object {
        const val BLANK_SERIAL = " "
        const val INVALID_BATTERY_PERCENTAGE = 101
    }
}
