package dev.pocketportal.infrastructure.adb

import dev.pocketportal.domain.device.AndroidDeviceState
import kotlin.test.Test
import kotlin.test.assertEquals

class AdbDeviceParserTest {
    @Test
    fun `parses connected and unauthorized devices`() {
        val output = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            $ONLINE_SERIAL device product:$PRODUCT model:$MODEL transport_id:$TRANSPORT_ID
            $UNAUTHORIZED_SERIAL unauthorized usb:$USB_LOCATION
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)

        assertEquals(EXPECTED_DEVICE_COUNT, devices.size)
        assertEquals(ONLINE_SERIAL, devices.first().serial.value)
        assertEquals(AndroidDeviceState.ONLINE, devices.first().state)
        assertEquals(MODEL, devices.first().model)
        assertEquals(PRODUCT, devices.first().product)
        assertEquals(UNAUTHORIZED_SERIAL, devices.last().serial.value)
        assertEquals(AndroidDeviceState.UNAUTHORIZED, devices.last().state)
    }

    private companion object {
        const val ONLINE_SERIAL = "online-serial"
        const val UNAUTHORIZED_SERIAL = "unauthorized-serial"
        const val PRODUCT = "pixel_product"
        const val MODEL = "Pixel_8"
        const val TRANSPORT_ID = "1"
        const val USB_LOCATION = "1-1"
        const val EXPECTED_DEVICE_COUNT = 2
    }
}
