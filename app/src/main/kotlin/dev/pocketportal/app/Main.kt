package dev.pocketportal.app

import dev.pocketportal.application.status.GetSystemStatusUseCase
import dev.pocketportal.application.device.GetAndroidDevicesUseCase
import dev.pocketportal.application.device.GetAndroidDeviceScreenshotUseCase
import dev.pocketportal.application.diagnostics.RunHostDiagnosticsUseCase
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceGateway
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceScreenshotGateway
import dev.pocketportal.infrastructure.diagnostics.LinuxHostDiagnosticsGateway
import dev.pocketportal.infrastructure.time.SystemClock
import dev.pocketportal.web.pocketPortalWeb
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = PocketPortalConfigLoader.load()
    val requestedCommand = args.firstOrNull()

    if (requestedCommand == CliConstants.DOCTOR_COMMAND) {
        val exitCode = runDoctor(
            RunHostDiagnosticsUseCase(
                gateway = LinuxHostDiagnosticsGateway(
                    adbPath = config.adb.executablePath,
                    commandTimeout = config.adb.timeout,
                ),
            ),
        )
        exitProcess(exitCode)
    }

    require(args.isEmpty()) {
        "Unknown PocketPortal command: $requestedCommand"
    }

    val getSystemStatus = GetSystemStatusUseCase(clock = SystemClock())
    val getAndroidDevices = GetAndroidDevicesUseCase(
        gateway = AdbAndroidDeviceGateway(
            adbPath = config.adb.executablePath,
            timeout = config.adb.timeout,
        ),
    )
    val getAndroidDeviceScreenshot = GetAndroidDeviceScreenshotUseCase(
        gateway = AdbAndroidDeviceScreenshotGateway(
            adbPath = config.adb.executablePath,
            timeout = config.adb.timeout,
            maximumBytes = config.adb.screenshotMaximumBytes,
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
            getAndroidDeviceScreenshot = getAndroidDeviceScreenshot,
        )
    }.start(wait = true)
}
