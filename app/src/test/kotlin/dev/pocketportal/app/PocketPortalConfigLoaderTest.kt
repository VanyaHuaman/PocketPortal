package dev.pocketportal.app

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private companion object {
        const val FILE_HOST = "file-host"
        const val FILE_PORT = 7000
        const val ENVIRONMENT_HOST = "environment-host"
        const val ENVIRONMENT_PORT = 9000
        const val FILE_ADB_PATH = "test-adb"
        const val FILE_ADB_TIMEOUT_MILLIS = 4000L
        const val SCREENSHOT_MAXIMUM_BYTES = 8_388_608L
    }
}
