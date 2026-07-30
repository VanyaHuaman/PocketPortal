package dev.pocketportal.app

import dev.pocketportal.application.status.GetSystemStatusUseCase
import dev.pocketportal.application.device.GetAndroidDevicesUseCase
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceGateway
import dev.pocketportal.infrastructure.time.SystemClock
import dev.pocketportal.web.pocketPortalWeb
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main() {
    val config = PocketPortalConfigLoader.load()
    val getSystemStatus = GetSystemStatusUseCase(clock = SystemClock())
    val getAndroidDevices = GetAndroidDevicesUseCase(
        gateway = AdbAndroidDeviceGateway(
            adbPath = config.adb.executablePath,
            timeout = config.adb.timeout,
        ),
    )

    embeddedServer(
        factory = CIO,
        port = config.port,
        host = config.host,
    ) {
        pocketPortalWeb(
            getSystemStatus = getSystemStatus,
            getAndroidDevices = getAndroidDevices,
        )
    }.start(wait = true)
}
