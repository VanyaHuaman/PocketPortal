package dev.pocketportal.app

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.time.Duration

data class PocketPortalConfig(
    val host: String,
    val port: Int,
    val adb: AdbConfig,
)

data class AdbConfig(
    val executablePath: String,
    val timeout: Duration,
)

fun interface EnvironmentReader {
    fun read(name: String): String?
}

object PocketPortalConfigLoader {
    fun load(
        environment: EnvironmentReader = EnvironmentReader(System::getenv),
    ): PocketPortalConfig {
        val configPath = environment.read(AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE)
        val properties = if (configPath == null) {
            loadPackagedProperties()
        } else {
            loadProperties(Path.of(configPath))
        }
        val host = environment.read(AppConstants.HOST_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.HOST_PROPERTY)
        val portText = environment.read(AppConstants.PORT_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.PORT_PROPERTY)
        val adbPath = environment.read(AppConstants.ADB_PATH_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.ADB_PATH_PROPERTY)
        val adbTimeoutText = environment.read(AppConstants.ADB_TIMEOUT_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.ADB_TIMEOUT_PROPERTY)

        return PocketPortalConfig(
            host = host,
            port = portText.toIntOrNull()
                ?: error("${AppConstants.PORT_PROPERTY} must be a valid integer"),
            adb = AdbConfig(
                executablePath = adbPath,
                timeout = adbTimeoutText.toPositiveDurationMillis(AppConstants.ADB_TIMEOUT_PROPERTY),
            ),
        )
    }

    private fun loadProperties(path: Path): Properties {
        require(Files.isRegularFile(path)) {
            "PocketPortal config file does not exist: ${path.toAbsolutePath()}"
        }

        return Properties().apply {
            Files.newInputStream(path).use(::load)
        }
    }

    private fun loadPackagedProperties(): Properties {
        val stream = PocketPortalConfigLoader::class.java.classLoader
            .getResourceAsStream(AppConstants.DEFAULT_CONFIG_RESOURCE)
            ?: error(
                "Packaged PocketPortal configuration is missing: " +
                    AppConstants.DEFAULT_CONFIG_RESOURCE,
            )

        return Properties().apply {
            stream.use(::load)
        }
    }

    private fun Properties.required(name: String): String =
        getProperty(name)?.takeIf(String::isNotBlank)
            ?: error("Missing required PocketPortal configuration: $name")

    private fun String.toPositiveDurationMillis(name: String): Duration {
        val millis = toLongOrNull()
            ?: error("$name must be a valid integer")
        require(millis >= MINIMUM_TIMEOUT_MILLIS) { "$name must be greater than zero" }
        return Duration.ofMillis(millis)
    }

    private const val MINIMUM_TIMEOUT_MILLIS = 1L
}
