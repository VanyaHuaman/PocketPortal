package dev.pocketportal.web

import dev.pocketportal.application.status.GetSystemStatus
import dev.pocketportal.application.device.DeviceDiscoveryFailure
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.device.GetAndroidDevices
import dev.pocketportal.domain.ServiceState
import dev.pocketportal.domain.SystemStatus
import dev.pocketportal.domain.PocketPortalConstants
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
    }
}
