package dev.pocketportal.infrastructure.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdbAndroidAdbBridgeGatewayTest {
    @Test
    fun `extracts the device source address from an Android route`() {
        val address = AdbAndroidAdbBridgeGateway.parseDeviceAddress(
            "192.168.0.0/24 dev wlan0 proto kernel scope link src 192.168.0.42",
        )

        assertEquals("192.168.0.42", address)
    }

    @Test
    fun `rejects missing and malformed source addresses`() {
        assertNull(
            AdbAndroidAdbBridgeGateway.parseDeviceAddress(
                "192.168.0.0/24 dev wlan0 proto kernel scope link",
            ),
        )
        assertNull(
            AdbAndroidAdbBridgeGateway.parseDeviceAddress(
                "default via gateway dev wlan0 src not-an-address",
            ),
        )
    }
}
