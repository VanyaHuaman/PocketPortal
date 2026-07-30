package dev.pocketportal.infrastructure.adb

import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.AndroidConnectionType
import kotlin.test.Test
import kotlin.test.assertEquals

class AdbDeviceParserTest {
    @Test
    fun `parses connected and unauthorized devices`() {
        val output = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            $ONLINE_SERIAL device usb:$USB_LOCATION product:$PRODUCT model:$MODEL transport_id:$TRANSPORT_ID
            $UNAUTHORIZED_SERIAL unauthorized usb:$USB_LOCATION
        """.trimIndent()

        val devices = AdbDeviceParser.parse(output)

        assertEquals(EXPECTED_DEVICE_COUNT, devices.size)
        assertEquals(ONLINE_SERIAL, devices.first().serial.value)
        assertEquals(AndroidDeviceState.ONLINE, devices.first().state)
        assertEquals(MODEL, devices.first().model)
        assertEquals(PRODUCT, devices.first().product)
        assertEquals(AndroidConnectionType.USB, devices.first().connectionType)
        assertEquals(AdbConstants.UNKNOWN_OBSERVATION_TIME_EPOCH_MILLIS, devices.first().observedAtEpochMillis)
        assertEquals(UNAUTHORIZED_SERIAL, devices.last().serial.value)
        assertEquals(AndroidDeviceState.UNAUTHORIZED, devices.last().state)
    }

    @Test
    fun `identifies a wireless adb serial`() {
        val output = """
            ${AdbConstants.DEVICE_LIST_HEADER}
            192.168.1.20:5555 device product:$PRODUCT model:$MODEL
        """.trimIndent()

        val device = AdbDeviceParser.parse(output, OBSERVED_AT).single()

        assertEquals(AndroidConnectionType.WIRELESS, device.connectionType)
        assertEquals(OBSERVED_AT, device.observedAtEpochMillis)
    }

    private companion object {
        const val ONLINE_SERIAL = "online-serial"
        const val UNAUTHORIZED_SERIAL = "unauthorized-serial"
        const val PRODUCT = "pixel_product"
        const val MODEL = "Pixel_8"
        const val TRANSPORT_ID = "1"
        const val USB_LOCATION = "1-1"
        const val EXPECTED_DEVICE_COUNT = 2
        const val OBSERVED_AT = 456L
    }
}
