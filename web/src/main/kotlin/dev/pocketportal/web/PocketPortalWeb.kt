package dev.pocketportal.web

import dev.pocketportal.application.status.GetSystemStatus
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.device.GetAndroidDevices
import dev.pocketportal.application.device.GetAndroidDeviceScreenshot
import dev.pocketportal.application.device.DeviceScreenshotFailure
import dev.pocketportal.application.device.DeviceScreenshotResult
import dev.pocketportal.domain.device.AndroidDevice
import dev.pocketportal.domain.device.DeviceSerial
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import kotlinx.serialization.Serializable

fun Application.pocketPortalWeb(
    getSystemStatus: GetSystemStatus,
    getAndroidDevices: GetAndroidDevices,
    getAndroidDeviceScreenshot: GetAndroidDeviceScreenshot,
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

        get(WebConstants.DEVICE_SCREENSHOT_PATH) {
            val serialValue = call.parameters[WebConstants.DEVICE_SERIAL_PARAMETER]
            val serial = try {
                DeviceSerial(requireNotNull(serialValue))
            } catch (_: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        WebConstants.SCREENSHOT_UNAVAILABLE_CODE,
                        DeviceScreenshotFailure.DEVICE_NOT_FOUND.name.lowercase(),
                    ),
                )
                return@get
            }

            when (val result = getAndroidDeviceScreenshot(serial)) {
                is DeviceScreenshotResult.Available -> {
                    call.response.headers.append(
                        WebConstants.OBSERVED_AT_HEADER,
                        result.observedAtEpochMillis.toString(),
                    )
                    call.respondBytes(
                        bytes = result.pngBytes,
                        contentType = ContentType.Image.PNG,
                        status = HttpStatusCode.OK,
                    )
                }
                is DeviceScreenshotResult.Unavailable ->
                    call.respond(
                        status = result.reason.toHttpStatus(),
                        message = ErrorResponse(
                            code = WebConstants.SCREENSHOT_UNAVAILABLE_CODE,
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

private fun DeviceScreenshotFailure.toHttpStatus(): HttpStatusCode = when (this) {
    DeviceScreenshotFailure.DEVICE_NOT_FOUND -> HttpStatusCode.NotFound
    DeviceScreenshotFailure.DEVICE_NOT_ONLINE -> HttpStatusCode.Conflict
    DeviceScreenshotFailure.OUTPUT_TOO_LARGE -> HttpStatusCode.PayloadTooLarge
    DeviceScreenshotFailure.TOOL_NOT_FOUND,
    DeviceScreenshotFailure.TIMED_OUT,
    DeviceScreenshotFailure.COMMAND_FAILED,
    -> HttpStatusCode.ServiceUnavailable
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
