package dev.pocketportal.app

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.time.Duration

data class PocketPortalConfig(
    val host: String,
    val port: Int,
    val adb: AdbConfig,
    val tls: TlsConfig,
)

data class TlsConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val keyStorePath: Path?,
    val keyAlias: String,
    val keyStorePassword: String?,
    val privateKeyPassword: String?,
)

data class AdbConfig(
    val executablePath: String,
    val timeout: Duration,
    val screenshotMaximumBytes: Long,
    val bridge: AdbBridgeConfig,
)

data class AdbBridgeConfig(
    val enabled: Boolean,
    val token: String?,
)

fun interface EnvironmentReader {
    fun read(name: String): String?
}

object PocketPortalConfigLoader {
    fun load(
        environment: EnvironmentReader = EnvironmentReader(System::getenv),
    ): PocketPortalConfig {
        val configPath = environment.read(AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE)
        val properties = when {
            configPath != null -> loadProperties(Path.of(configPath))
            else -> defaultUserConfigPath(environment)
                ?.takeIf(Files::isRegularFile)
                ?.let(::loadProperties)
                ?: loadPackagedProperties()
        }
        val host = environment.read(AppConstants.HOST_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.HOST_PROPERTY)
        val portText = environment.read(AppConstants.PORT_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.PORT_PROPERTY)
        val adbPath = environment.read(AppConstants.ADB_PATH_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.ADB_PATH_PROPERTY)
        val adbTimeoutText = environment.read(AppConstants.ADB_TIMEOUT_ENVIRONMENT_VARIABLE)
            ?: properties.required(AppConstants.ADB_TIMEOUT_PROPERTY)
        val screenshotMaximumBytesText =
            environment.read(AppConstants.SCREENSHOT_MAXIMUM_BYTES_ENVIRONMENT_VARIABLE)
                ?: properties.getProperty(AppConstants.SCREENSHOT_MAXIMUM_BYTES_PROPERTY)
                ?: AppConstants.DEFAULT_SCREENSHOT_MAXIMUM_BYTES.toString()
        val bridgeEnabledText =
            environment.read(AppConstants.ADB_BRIDGE_ENABLED_ENVIRONMENT_VARIABLE)
                ?: properties.getProperty(AppConstants.ADB_BRIDGE_ENABLED_PROPERTY)
                ?: AppConstants.FALSE_VALUE
        val bridgeEnabled = bridgeEnabledText.toStrictBoolean(
            AppConstants.ADB_BRIDGE_ENABLED_PROPERTY,
        )
        val bridgeToken = environment.read(AppConstants.ADB_BRIDGE_TOKEN_ENVIRONMENT_VARIABLE)
            ?.takeIf(String::isNotBlank)
        require(!bridgeEnabled || bridgeToken != null) {
            "${AppConstants.ADB_BRIDGE_TOKEN_ENVIRONMENT_VARIABLE} is required when " +
                "${AppConstants.ADB_BRIDGE_ENABLED_PROPERTY} is enabled"
        }
        require(
            !bridgeEnabled ||
                requireNotNull(bridgeToken).length >=
                AppConstants.MINIMUM_ADB_BRIDGE_TOKEN_CHARACTERS,
        ) {
            "${AppConstants.ADB_BRIDGE_TOKEN_ENVIRONMENT_VARIABLE} must contain at least " +
                "${AppConstants.MINIMUM_ADB_BRIDGE_TOKEN_CHARACTERS} characters"
        }
        val tlsEnabled = (
            environment.read(AppConstants.TLS_ENABLED_ENVIRONMENT_VARIABLE)
                ?: properties.getProperty(AppConstants.TLS_ENABLED_PROPERTY)
                ?: AppConstants.FALSE_VALUE
            ).toStrictBoolean(AppConstants.TLS_ENABLED_PROPERTY)
        val tlsPort = (
            environment.read(AppConstants.TLS_PORT_ENVIRONMENT_VARIABLE)
                ?: properties.getProperty(AppConstants.TLS_PORT_PROPERTY)
                ?: AppConstants.DEFAULT_TLS_PORT.toString()
            ).toPort(AppConstants.TLS_PORT_PROPERTY)
        val tlsHost = environment.read(AppConstants.TLS_HOST_ENVIRONMENT_VARIABLE)
            ?: properties.getProperty(AppConstants.TLS_HOST_PROPERTY)
            ?: host
        val tlsKeyStorePath = (
            environment.read(AppConstants.TLS_KEY_STORE_PATH_ENVIRONMENT_VARIABLE)
                ?: properties.getProperty(AppConstants.TLS_KEY_STORE_PATH_PROPERTY)
            )?.takeIf(String::isNotBlank)?.let(Path::of)
        val tlsKeyAlias = environment.read(AppConstants.TLS_KEY_ALIAS_ENVIRONMENT_VARIABLE)
            ?: properties.getProperty(AppConstants.TLS_KEY_ALIAS_PROPERTY)
            ?: AppConstants.DEFAULT_TLS_KEY_ALIAS
        val tlsKeyStorePassword =
            environment.read(AppConstants.TLS_KEY_STORE_PASSWORD_ENVIRONMENT_VARIABLE)
                ?.takeIf(String::isNotBlank)
        val tlsPrivateKeyPassword =
            environment.read(AppConstants.TLS_PRIVATE_KEY_PASSWORD_ENVIRONMENT_VARIABLE)
                ?.takeIf(String::isNotBlank)
                ?: tlsKeyStorePassword
        require(
            !tlsEnabled ||
                (
                    tlsKeyStorePath != null &&
                        Files.isRegularFile(tlsKeyStorePath) &&
                        tlsKeyStorePassword != null &&
                        tlsPrivateKeyPassword != null
                    )
        ) {
            "TLS requires an existing key store path plus key-store and private-key passwords"
        }

        return PocketPortalConfig(
            host = host,
            port = portText.toIntOrNull()
                ?: error("${AppConstants.PORT_PROPERTY} must be a valid integer"),
            adb = AdbConfig(
                executablePath = adbPath,
                timeout = adbTimeoutText.toPositiveDurationMillis(AppConstants.ADB_TIMEOUT_PROPERTY),
                screenshotMaximumBytes = screenshotMaximumBytesText.toPositiveLong(
                    AppConstants.SCREENSHOT_MAXIMUM_BYTES_PROPERTY,
                ),
                bridge = AdbBridgeConfig(
                    enabled = bridgeEnabled,
                    token = bridgeToken,
                ),
            ),
            tls = TlsConfig(
                enabled = tlsEnabled,
                host = tlsHost,
                port = tlsPort,
                keyStorePath = tlsKeyStorePath,
                keyAlias = tlsKeyAlias,
                keyStorePassword = tlsKeyStorePassword,
                privateKeyPassword = tlsPrivateKeyPassword,
            ),
        )
    }

    private fun defaultUserConfigPath(environment: EnvironmentReader): Path? {
        val xdgConfigHome = environment.read(AppConstants.XDG_CONFIG_HOME_ENVIRONMENT_VARIABLE)
        if (!xdgConfigHome.isNullOrBlank()) {
            return Path.of(xdgConfigHome).resolve(AppConstants.XDG_CONFIG_RELATIVE_PATH)
        }

        return environment.read(AppConstants.HOME_ENVIRONMENT_VARIABLE)
            ?.takeIf(String::isNotBlank)
            ?.let(Path::of)
            ?.resolve(AppConstants.USER_CONFIG_RELATIVE_PATH)
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

    private fun String.toPositiveLong(name: String): Long {
        val value = toLongOrNull() ?: error("$name must be a valid integer")
        require(value >= MINIMUM_POSITIVE_VALUE) { "$name must be greater than zero" }
        return value
    }

    private fun String.toStrictBoolean(name: String): Boolean = when (lowercase()) {
        AppConstants.TRUE_VALUE -> true
        AppConstants.FALSE_VALUE -> false
        else -> error("$name must be true or false")
    }

    private fun String.toPort(name: String): Int {
        val value = toIntOrNull() ?: error("$name must be a valid integer")
        require(value in MINIMUM_PORT..MAXIMUM_PORT) {
            "$name must be between $MINIMUM_PORT and $MAXIMUM_PORT"
        }
        return value
    }

    private const val MINIMUM_TIMEOUT_MILLIS = 1L
    private const val MINIMUM_POSITIVE_VALUE = 1L
    private const val MINIMUM_PORT = 1
    private const val MAXIMUM_PORT = 65_535
}
