package dev.pocketportal.app

import dev.pocketportal.application.status.GetSystemStatusUseCase
import dev.pocketportal.application.device.GetAndroidDevicesUseCase
import dev.pocketportal.application.device.GetAndroidDeviceScreenshotUseCase
import dev.pocketportal.application.device.WakeAndroidDeviceUseCase
import dev.pocketportal.application.diagnostics.RunHostDiagnosticsUseCase
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceGateway
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceScreenshotGateway
import dev.pocketportal.infrastructure.adb.AdbAndroidDeviceWakeGateway
import dev.pocketportal.infrastructure.adb.AdbAndroidAdbBridgeGateway
import dev.pocketportal.application.device.OpenAndroidAdbBridgeUseCase
import dev.pocketportal.infrastructure.diagnostics.LinuxHostDiagnosticsGateway
import dev.pocketportal.infrastructure.time.SystemClock
import dev.pocketportal.web.pocketPortalWeb
import io.ktor.server.netty.Netty
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.sslConnector
import io.ktor.server.engine.applicationEnvironment
import java.nio.file.Files
import java.security.KeyStore
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
    val wakeAndroidDevice = WakeAndroidDeviceUseCase(
        gateway = AdbAndroidDeviceWakeGateway(
            adbPath = config.adb.executablePath,
            timeout = config.adb.timeout,
        ),
    )
    val openAndroidAdbBridge = if (config.adb.bridge.enabled) {
        OpenAndroidAdbBridgeUseCase(
            gateway = AdbAndroidAdbBridgeGateway(
                adbPath = config.adb.executablePath,
                timeout = config.adb.timeout,
            ),
        )
    } else {
        null
    }

    embeddedServer(
        factory = Netty,
        environment = applicationEnvironment(),
        configure = {
            connector {
                host = config.host
                port = config.port
            }
            if (config.tls.enabled) {
                val keyStorePath = requireNotNull(config.tls.keyStorePath)
                val keyStorePassword = requireNotNull(config.tls.keyStorePassword)
                val privateKeyPassword = requireNotNull(config.tls.privateKeyPassword)
                val keyStore = KeyStore.getInstance(KEY_STORE_TYPE).apply {
                    Files.newInputStream(keyStorePath).use { input ->
                        load(input, keyStorePassword.toCharArray())
                    }
                }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = config.tls.keyAlias,
                    keyStorePassword = { keyStorePassword.toCharArray() },
                    privateKeyPassword = { privateKeyPassword.toCharArray() },
                ) {
                    host = config.tls.host
                    port = config.tls.port
                    this.keyStorePath = keyStorePath.toFile()
                }
            }
        },
    ) {
        pocketPortalWeb(
            getSystemStatus = getSystemStatus,
            getAndroidDevices = getAndroidDevices,
            getAndroidDeviceScreenshot = getAndroidDeviceScreenshot,
            wakeAndroidDevice = wakeAndroidDevice,
            openAndroidAdbBridge = openAndroidAdbBridge,
            adbBridgeToken = config.adb.bridge.token,
        )
    }.start(wait = true)
}

private const val KEY_STORE_TYPE = "PKCS12"
