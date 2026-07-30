package dev.pocketportal.web

import dev.pocketportal.application.status.GetSystemStatus
import dev.pocketportal.application.device.DeviceDiscoveryResult
import dev.pocketportal.application.device.GetAndroidDevices
import dev.pocketportal.application.device.GetAndroidDeviceScreenshot
import dev.pocketportal.application.device.DeviceScreenshotFailure
import dev.pocketportal.application.device.DeviceScreenshotResult
import dev.pocketportal.application.device.DeviceWakeFailure
import dev.pocketportal.application.device.DeviceWakeResult
import dev.pocketportal.application.device.WakeAndroidDevice
import dev.pocketportal.application.device.AndroidAdbBridgeResult
import dev.pocketportal.application.device.OpenAndroidAdbBridge
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
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.security.MessageDigest

fun Application.pocketPortalWeb(
    getSystemStatus: GetSystemStatus,
    getAndroidDevices: GetAndroidDevices,
    getAndroidDeviceScreenshot: GetAndroidDeviceScreenshot,
    wakeAndroidDevice: WakeAndroidDevice,
    openAndroidAdbBridge: OpenAndroidAdbBridge? = null,
    adbBridgeToken: String? = null,
) {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)

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

        post(WebConstants.DEVICE_WAKE_PATH) {
            val serial = call.deviceSerialOrBadRequest(WebConstants.DEVICE_WAKE_FAILED_CODE)
                ?: return@post
            when (val result = wakeAndroidDevice(serial)) {
                DeviceWakeResult.Completed -> call.respond(HttpStatusCode.NoContent)
                is DeviceWakeResult.Failed ->
                    call.respond(
                        status = result.reason.toHttpStatus(),
                        message = ErrorResponse(
                            code = WebConstants.DEVICE_WAKE_FAILED_CODE,
                            detail = result.reason.name.lowercase(),
                        ),
                    )
            }
        }

        webSocket(WebConstants.DEVICE_ADB_BRIDGE_PATH) {
            val configuredToken = adbBridgeToken
            val suppliedToken = call.request.headers[WebConstants.AUTHORIZATION_HEADER]
                ?.takeIf { it.startsWith(WebConstants.BEARER_PREFIX) }
                ?.removePrefix(WebConstants.BEARER_PREFIX)
            if (
                configuredToken.isNullOrBlank() ||
                suppliedToken == null ||
                !MessageDigest.isEqual(
                    configuredToken.encodeToByteArray(),
                    suppliedToken.encodeToByteArray(),
                )
            ) {
                close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        WebConstants.ADB_BRIDGE_UNAUTHORIZED_REASON,
                    ),
                )
                return@webSocket
            }

            val serial = call.parameters[WebConstants.DEVICE_SERIAL_PARAMETER]
                ?.let {
                    try {
                        DeviceSerial(it)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            val bridgeResult = if (serial == null || openAndroidAdbBridge == null) {
                null
            } else {
                openAndroidAdbBridge(serial)
            }
            val bridge = (bridgeResult as? AndroidAdbBridgeResult.Opened)?.bridge
            if (bridge == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.CANNOT_ACCEPT,
                        WebConstants.ADB_BRIDGE_UNAVAILABLE_REASON,
                    ),
                )
                return@webSocket
            }

            val deviceToClient = launch {
                val buffer = ByteArray(WebConstants.ADB_BRIDGE_BUFFER_BYTES)
                while (true) {
                    val count = bridge.read(buffer)
                    if (count < 0) break
                    send(Frame.Binary(fin = true, data = buffer.copyOf(count)))
                }
            }
            try {
                for (frame in incoming) {
                    if (frame is Frame.Binary) {
                        bridge.write(frame.readBytes())
                    }
                }
            } finally {
                deviceToClient.cancelAndJoin()
                bridge.close()
            }
        }

        staticResources(
            remotePath = WebConstants.FRONTEND_ROUTE,
            basePackage = WebConstants.FRONTEND_RESOURCE_PACKAGE,
            index = WebConstants.FRONTEND_INDEX_FILE,
        )
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.deviceSerialOrBadRequest(
    errorCode: String,
): DeviceSerial? {
    val serialValue = parameters[WebConstants.DEVICE_SERIAL_PARAMETER]
    return try {
        DeviceSerial(requireNotNull(serialValue))
    } catch (_: IllegalArgumentException) {
        respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(errorCode, DeviceWakeFailure.DEVICE_NOT_FOUND.name.lowercase()),
        )
        null
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

private fun DeviceWakeFailure.toHttpStatus(): HttpStatusCode = when (this) {
    DeviceWakeFailure.DEVICE_NOT_FOUND -> HttpStatusCode.NotFound
    DeviceWakeFailure.DEVICE_NOT_ONLINE -> HttpStatusCode.Conflict
    DeviceWakeFailure.TOOL_NOT_FOUND,
    DeviceWakeFailure.TIMED_OUT,
    DeviceWakeFailure.COMMAND_FAILED,
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
    val formFactor: String,
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
    formFactor = details?.formFactor?.name?.lowercase() ?: WebConstants.UNKNOWN_VALUE,
    observedAtEpochMillis = observedAtEpochMillis,
)
