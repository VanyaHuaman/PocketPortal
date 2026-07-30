package dev.pocketportal.app

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PocketPortalConfigLoaderTest {
    @Test
    fun `environment overrides file configuration`() {
        val configFile = Files.createTempFile("pocketportal-config", ".properties")
        configFile.writeText(
            """
            ${AppConstants.HOST_PROPERTY}=$FILE_HOST
            ${AppConstants.PORT_PROPERTY}=$FILE_PORT
            ${AppConstants.ADB_PATH_PROPERTY}=$FILE_ADB_PATH
            ${AppConstants.ADB_TIMEOUT_PROPERTY}=$FILE_ADB_TIMEOUT_MILLIS
            ${AppConstants.SCREENSHOT_MAXIMUM_BYTES_PROPERTY}=$SCREENSHOT_MAXIMUM_BYTES
            """.trimIndent(),
        )
        val environmentValues = mapOf(
            AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE to configFile.toString(),
            AppConstants.HOST_ENVIRONMENT_VARIABLE to ENVIRONMENT_HOST,
            AppConstants.PORT_ENVIRONMENT_VARIABLE to ENVIRONMENT_PORT.toString(),
        )

        val config = PocketPortalConfigLoader.load(
            environment = EnvironmentReader(environmentValues::get),
        )

        assertEquals(ENVIRONMENT_HOST, config.host)
        assertEquals(ENVIRONMENT_PORT, config.port)
        assertEquals(FILE_ADB_PATH, config.adb.executablePath)
        assertEquals(FILE_ADB_TIMEOUT_MILLIS, config.adb.timeout.toMillis())
        assertEquals(SCREENSHOT_MAXIMUM_BYTES, config.adb.screenshotMaximumBytes)
    }

    @Test
    fun `uses the conventional user config when no explicit path is set`() {
        val configHome = Files.createTempDirectory("pocketportal-config-home")
        val configFile = configHome.resolve(AppConstants.XDG_CONFIG_RELATIVE_PATH)
        Files.createDirectories(configFile.parent)
        configFile.writeText(
            """
            ${AppConstants.HOST_PROPERTY}=$FILE_HOST
            ${AppConstants.PORT_PROPERTY}=$FILE_PORT
            ${AppConstants.ADB_PATH_PROPERTY}=$FILE_ADB_PATH
            ${AppConstants.ADB_TIMEOUT_PROPERTY}=$FILE_ADB_TIMEOUT_MILLIS
            ${AppConstants.SCREENSHOT_MAXIMUM_BYTES_PROPERTY}=$SCREENSHOT_MAXIMUM_BYTES
            """.trimIndent(),
        )

        val config = PocketPortalConfigLoader.load(
            environment = EnvironmentReader { name ->
                if (name == AppConstants.XDG_CONFIG_HOME_ENVIRONMENT_VARIABLE) {
                    configHome.toString()
                } else {
                    null
                }
            },
        )

        assertEquals(FILE_HOST, config.host)
        assertEquals(FILE_PORT, config.port)
        assertEquals(FILE_ADB_PATH, config.adb.executablePath)
        assertEquals(SCREENSHOT_MAXIMUM_BYTES, config.adb.screenshotMaximumBytes)
    }

    @Test
    fun `requires a token when the adb bridge is enabled`() {
        val configFile = Files.createTempFile("pocketportal-bridge-config", ".properties")
        configFile.writeText(
            """
            ${AppConstants.HOST_PROPERTY}=$FILE_HOST
            ${AppConstants.PORT_PROPERTY}=$FILE_PORT
            ${AppConstants.ADB_PATH_PROPERTY}=$FILE_ADB_PATH
            ${AppConstants.ADB_TIMEOUT_PROPERTY}=$FILE_ADB_TIMEOUT_MILLIS
            ${AppConstants.ADB_BRIDGE_ENABLED_PROPERTY}=true
            """.trimIndent(),
        )

        assertFailsWith<IllegalArgumentException> {
            PocketPortalConfigLoader.load(
                environment = EnvironmentReader { name ->
                    if (name == AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE) {
                        configFile.toString()
                    } else {
                        null
                    }
                },
            )
        }
    }

    @Test
    fun `loads enabled adb bridge token only from the environment`() {
        val configFile = Files.createTempFile("pocketportal-bridge-config", ".properties")
        configFile.writeText(
            """
            ${AppConstants.HOST_PROPERTY}=$FILE_HOST
            ${AppConstants.PORT_PROPERTY}=$FILE_PORT
            ${AppConstants.ADB_PATH_PROPERTY}=$FILE_ADB_PATH
            ${AppConstants.ADB_TIMEOUT_PROPERTY}=$FILE_ADB_TIMEOUT_MILLIS
            ${AppConstants.ADB_BRIDGE_ENABLED_PROPERTY}=true
            """.trimIndent(),
        )
        val environmentValues = mapOf(
            AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE to configFile.toString(),
            AppConstants.ADB_BRIDGE_TOKEN_ENVIRONMENT_VARIABLE to BRIDGE_TOKEN,
        )

        val config = PocketPortalConfigLoader.load(
            environment = EnvironmentReader(environmentValues::get),
        )

        assertEquals(true, config.adb.bridge.enabled)
        assertEquals(BRIDGE_TOKEN, config.adb.bridge.token)
    }

    @Test
    fun `loads enabled tls from an external key store and secret environment`() {
        val keyStore = Files.createTempFile("pocketportal", ".p12")
        val configFile = Files.createTempFile("pocketportal-tls-config", ".properties")
        configFile.writeText(
            """
            ${AppConstants.HOST_PROPERTY}=$FILE_HOST
            ${AppConstants.PORT_PROPERTY}=$FILE_PORT
            ${AppConstants.ADB_PATH_PROPERTY}=$FILE_ADB_PATH
            ${AppConstants.ADB_TIMEOUT_PROPERTY}=$FILE_ADB_TIMEOUT_MILLIS
            ${AppConstants.TLS_ENABLED_PROPERTY}=true
            ${AppConstants.TLS_HOST_PROPERTY}=$TLS_HOST
            ${AppConstants.TLS_PORT_PROPERTY}=$TLS_PORT
            ${AppConstants.TLS_KEY_STORE_PATH_PROPERTY}=$keyStore
            """.trimIndent(),
        )
        val environmentValues = mapOf(
            AppConstants.CONFIG_PATH_ENVIRONMENT_VARIABLE to configFile.toString(),
            AppConstants.TLS_KEY_STORE_PASSWORD_ENVIRONMENT_VARIABLE to TLS_PASSWORD,
            AppConstants.TLS_PRIVATE_KEY_PASSWORD_ENVIRONMENT_VARIABLE to TLS_PASSWORD,
        )

        val config = PocketPortalConfigLoader.load(
            environment = EnvironmentReader(environmentValues::get),
        )

        assertEquals(true, config.tls.enabled)
        assertEquals(TLS_HOST, config.tls.host)
        assertEquals(TLS_PORT, config.tls.port)
        assertEquals(keyStore, config.tls.keyStorePath)
    }

    private companion object {
        const val FILE_HOST = "file-host"
        const val FILE_PORT = 7000
        const val ENVIRONMENT_HOST = "environment-host"
        const val ENVIRONMENT_PORT = 9000
        const val FILE_ADB_PATH = "test-adb"
        const val FILE_ADB_TIMEOUT_MILLIS = 4000L
        const val SCREENSHOT_MAXIMUM_BYTES = 8_388_608L
        const val BRIDGE_TOKEN = "test-bridge-token-with-32-characters"
        const val TLS_HOST = "192.168.0.151"
        const val TLS_PORT = 8443
        const val TLS_PASSWORD = "test-tls-password"
    }
}
