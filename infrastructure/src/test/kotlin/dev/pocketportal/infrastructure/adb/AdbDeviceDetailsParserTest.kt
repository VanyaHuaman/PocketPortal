package dev.pocketportal.infrastructure.adb

import dev.pocketportal.domain.device.AndroidChargingState
import dev.pocketportal.domain.device.AndroidScreenState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdbDeviceDetailsParserTest {
    @Test
    fun `parses android battery charging and screen details`() {
        val details = AdbDeviceDetailsParser.parse(COMPLETE_OUTPUT)

        assertEquals("Google", details.manufacturer)
        assertEquals("13", details.androidVersion)
        assertEquals(33, details.sdkLevel)
        assertEquals(82, details.batteryPercentage)
        assertEquals(AndroidChargingState.CHARGING, details.chargingState)
        assertEquals(AndroidScreenState.ON, details.screenState)
    }

    @Test
    fun `preserves partial data when fields are absent`() {
        val details = AdbDeviceDetailsParser.parse(
            """
            ${AdbConstants.MANUFACTURER_MARKER}
            Google
            ${AdbConstants.ANDROID_VERSION_MARKER}
            14
            ${AdbConstants.SDK_LEVEL_MARKER}
            ${AdbConstants.BATTERY_MARKER}
            ${AdbConstants.POWER_MARKER}
            """.trimIndent(),
        )

        assertEquals("Google", details.manufacturer)
        assertEquals("14", details.androidVersion)
        assertNull(details.sdkLevel)
        assertNull(details.batteryPercentage)
        assertEquals(AndroidChargingState.UNKNOWN, details.chargingState)
        assertEquals(AndroidScreenState.UNKNOWN, details.screenState)
    }

    private companion object {
        val COMPLETE_OUTPUT = """
            ${AdbConstants.MANUFACTURER_MARKER}
            Google
            ${AdbConstants.ANDROID_VERSION_MARKER}
            13
            ${AdbConstants.SDK_LEVEL_MARKER}
            33
            ${AdbConstants.BATTERY_MARKER}
              AC powered: false
              USB powered: true
              Wireless powered: false
              status: 2
              level: 82
              scale: 100
            ${AdbConstants.POWER_MARKER}
              mWakefulness=Awake
        """.trimIndent()
    }
}
