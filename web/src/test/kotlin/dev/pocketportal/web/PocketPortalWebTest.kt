package dev.pocketportal.web

import dev.pocketportal.application.status.GetSystemStatus
import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.device.GetAndroidDevices
import dev.pocketportal.domain.ServiceState
import dev.pocketportal.domain.SystemStatus
import dev.pocketportal.domain.PocketPortalConstants
import dev.pocketportal.domain.device.AndroidChargingState
import dev.pocketportal.domain.device.AndroidConnectionType
import dev.pocketportal.domain.device.AndroidDevice
import dev.pocketportal.domain.device.AndroidDeviceDetails
import dev.pocketportal.domain.device.AndroidDeviceState
import dev.pocketportal.domain.device.AndroidScreenState
import dev.pocketportal.domain.device.DeviceSerial
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PocketPortalWebTest {
    @Test
    fun `status endpoint maps the use case result to json`() = testApplication {
        application {
            pocketPortalWeb(
                getSystemStatus = GetSystemStatus {
                    SystemStatus(
                        service = PocketPortalConstants.SERVICE_NAME,
                        state = ServiceState.READY,
                        observedAtEpochMillis = OBSERVED_AT,
                    )
                },
                getAndroidDevices = GetAndroidDevices {
                    DeviceDiscoveryResult.Available(emptyList())
                },
            )
        }

        val response = client.get(WebConstants.STATUS_PATH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"state\":\"${WebConstants.READY_STATE}\""))
        assertTrue(response.bodyAsText().contains("\"observedAtEpochMillis\":$OBSERVED_AT"))
    }

    @Test
    fun `devices endpoint returns a typed unavailable response`() = testApplication {
        application {
            pocketPortalWeb(
                getSystemStatus = GetSystemStatus {
                    SystemStatus(
                        service = PocketPortalConstants.SERVICE_NAME,
                        state = ServiceState.READY,
                        observedAtEpochMillis = OBSERVED_AT,
                    )
                },
                getAndroidDevices = GetAndroidDevices {
                    DeviceDiscoveryResult.Unavailable(DeviceDiscoveryFailure.TOOL_NOT_FOUND)
                },
            )
        }

        val response = client.get(WebConstants.DEVICES_PATH)

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(
            response.bodyAsText()
                .contains("\"code\":\"${WebConstants.DEVICE_DISCOVERY_UNAVAILABLE_CODE}\""),
        )
        assertTrue(
            response.bodyAsText().contains(
                "\"detail\":\"${DeviceDiscoveryFailure.TOOL_NOT_FOUND.name.lowercase()}\"",
            ),
        )
    }

    @Test
    fun `devices endpoint maps enriched observations`() = testApplication {
        application {
            pocketPortalWeb(
                getSystemStatus = GetSystemStatus {
                    SystemStatus(
                        service = PocketPortalConstants.SERVICE_NAME,
                        state = ServiceState.READY,
                        observedAtEpochMillis = OBSERVED_AT,
                    )
                },
                getAndroidDevices = GetAndroidDevices {
                    DeviceDiscoveryResult.Available(
                        listOf(
                            AndroidDevice(
                                serial = DeviceSerial(DEVICE_SERIAL),
                                state = AndroidDeviceState.ONLINE,
                                model = DEVICE_MODEL,
                                product = DEVICE_PRODUCT,
                                connectionType = AndroidConnectionType.USB,
                                details = AndroidDeviceDetails(
                                    manufacturer = DEVICE_MANUFACTURER,
                                    androidVersion = ANDROID_VERSION,
                                    sdkLevel = SDK_LEVEL,
                                    batteryPercentage = BATTERY_PERCENTAGE,
                                    chargingState = AndroidChargingState.CHARGING,
                                    screenState = AndroidScreenState.ON,
                                ),
                                observedAtEpochMillis = OBSERVED_AT,
                            ),
                        ),
                    )
                },
            )
        }

        val body = client.get(WebConstants.DEVICES_PATH).bodyAsText()

        assertTrue(body.contains("\"manufacturer\":\"$DEVICE_MANUFACTURER\""))
        assertTrue(body.contains("\"androidVersion\":\"$ANDROID_VERSION\""))
        assertTrue(body.contains("\"batteryPercentage\":$BATTERY_PERCENTAGE"))
        assertTrue(body.contains("\"connectionType\":\"usb\""))
        assertTrue(body.contains("\"observedAtEpochMillis\":$OBSERVED_AT"))
    }

    @Test
    fun `root serves the dashboard`() = testApplication {
        application {
            pocketPortalWeb(
                getSystemStatus = GetSystemStatus {
                    SystemStatus(
                        service = PocketPortalConstants.SERVICE_NAME,
                        state = ServiceState.READY,
                        observedAtEpochMillis = OBSERVED_AT,
                    )
                },
                getAndroidDevices = GetAndroidDevices {
                    DeviceDiscoveryResult.Available(emptyList())
                },
            )
        }

        val response = client.get(WebConstants.FRONTEND_ROUTE)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("<title>PocketPortal</title>"))
    }

    private companion object {
        const val OBSERVED_AT = 789L
        const val DEVICE_SERIAL = "ABC123"
        const val DEVICE_MODEL = "Pixel_4_XL"
        const val DEVICE_PRODUCT = "coral"
        const val DEVICE_MANUFACTURER = "Google"
        const val ANDROID_VERSION = "13"
        const val SDK_LEVEL = 33
        const val BATTERY_PERCENTAGE = 75
    }
}
