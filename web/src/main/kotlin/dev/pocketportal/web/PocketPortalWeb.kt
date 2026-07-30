package dev.pocketportal.web

import dev.pocketportal.application.status.GetSystemStatus
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.device.GetAndroidDevices
import dev.pocketportal.domain.device.AndroidDevice
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import kotlinx.serialization.Serializable

fun Application.pocketPortalWeb(
    getSystemStatus: GetSystemStatus,
    getAndroidDevices: GetAndroidDevices,
) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get(WebConstants.STATUS_PATH) {
            val status = getSystemStatus()
            call.respond(
                status = HttpStatusCode.OK,
                message = SystemStatusResponse(
                    service = status.service,
                    state = status.state.name.lowercase(),
                    observedAtEpochMillis = status.observedAtEpochMillis,
                ),
            )
        }

        get(WebConstants.DEVICES_PATH) {
            when (val result = getAndroidDevices()) {
                is DeviceDiscoveryResult.Available ->
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = DeviceListResponse(
                            devices = result.devices.map(AndroidDevice::toResponse),
                        ),
                    )

                is DeviceDiscoveryResult.Unavailable ->
                    call.respond(
                        status = HttpStatusCode.ServiceUnavailable,
                        message = ErrorResponse(
                            code = WebConstants.DEVICE_DISCOVERY_UNAVAILABLE_CODE,
                            detail = result.reason.name.lowercase(),
                        ),
                    )
            }
        }

        staticResources(
            remotePath = WebConstants.FRONTEND_ROUTE,
            basePackage = WebConstants.FRONTEND_RESOURCE_PACKAGE,
            index = WebConstants.FRONTEND_INDEX_FILE,
        )
    }
}

@Serializable
data class SystemStatusResponse(
    val service: String,
    val state: String,
    val observedAtEpochMillis: Long,
)

@Serializable
data class DeviceListResponse(
    val devices: List<AndroidDeviceResponse>,
)

@Serializable
data class AndroidDeviceResponse(
    val serial: String,
    val state: String,
    val model: String?,
    val product: String?,
    val connectionType: String,
    val manufacturer: String?,
    val androidVersion: String?,
    val sdkLevel: Int?,
    val batteryPercentage: Int?,
    val chargingState: String,
    val screenState: String,
    val observedAtEpochMillis: Long,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val detail: String,
)

private fun AndroidDevice.toResponse(): AndroidDeviceResponse = AndroidDeviceResponse(
    serial = serial.value,
    state = state.name.lowercase(),
    model = model,
    product = product,
    connectionType = connectionType.name.lowercase(),
    manufacturer = details?.manufacturer,
    androidVersion = details?.androidVersion,
    sdkLevel = details?.sdkLevel,
    batteryPercentage = details?.batteryPercentage,
    chargingState = details?.chargingState?.name?.lowercase() ?: WebConstants.UNKNOWN_VALUE,
    screenState = details?.screenState?.name?.lowercase() ?: WebConstants.UNKNOWN_VALUE,
    observedAtEpochMillis = observedAtEpochMillis,
)
