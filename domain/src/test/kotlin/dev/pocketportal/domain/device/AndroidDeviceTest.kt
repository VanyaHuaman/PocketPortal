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

    private companion object {
        const val BLANK_SERIAL = " "
    }
}
