package dev.pocketportal.connect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.nio.file.Path

class ConnectConfigParserTest {
    @Test
    fun `parses a secure remote server`() {
        val config = ConnectConfigParser.parse(
            arrayOf(
                "--server",
                "wss://pocketportal.example",
                "--serial",
                "device-123",
            ),
            environment = { name ->
                if (name == ConnectConstants.TOKEN_ENVIRONMENT_VARIABLE) TOKEN else null
            },
        )

        assertEquals("wss://pocketportal.example", config.server.toString())
        assertEquals("device-123", config.serial)
        assertEquals(ConnectConstants.DEFAULT_LOCAL_PORT, config.localPort)
        assertEquals(TOKEN, config.token)
        assertEquals(null, config.caCertificatePath)
    }

    @Test
    fun `rejects an unencrypted non-loopback server`() {
        assertFailsWith<IllegalArgumentException> {
            ConnectConfigParser.parse(
                arrayOf(
                    "--server",
                    "ws://192.168.0.151:8080",
                    "--serial",
                    "device-123",
                ),
                environment = { TOKEN },
            )
        }
    }

    @Test
    fun `allows unencrypted loopback for development`() {
        val config = ConnectConfigParser.parse(
            arrayOf(
                "--server",
                "ws://127.0.0.1:8080",
                "--serial",
                "device-123",
            ),
            environment = { TOKEN },
        )

        assertEquals("ws://127.0.0.1:8080", config.server.toString())
    }

    @Test
    fun `accepts an additional pem ca bundle path`() {
        val config = ConnectConfigParser.parse(
            arrayOf(
                "--server",
                "wss://192.168.0.151:8443",
                "--serial",
                "device-123",
                "--ca-certificate",
                "/tmp/pocketportal-ca.pem",
            ),
            environment = { TOKEN },
        )

        assertEquals(Path.of("/tmp/pocketportal-ca.pem"), config.caCertificatePath)
    }

    private companion object {
        const val TOKEN = "test-token"
    }
}
