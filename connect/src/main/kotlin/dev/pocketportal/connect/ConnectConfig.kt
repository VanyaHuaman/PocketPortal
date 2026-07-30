package dev.pocketportal.connect

import java.net.URI
import java.nio.file.Path

internal data class ConnectConfig(
    val server: URI,
    val serial: String,
    val token: String,
    val localPort: Int,
    val adbPath: String,
    val caCertificatePath: Path?,
)

internal object ConnectConfigParser {
    fun parse(
        args: Array<String>,
        environment: (String) -> String? = System::getenv,
    ): ConnectConfig {
        val values = args.toList().windowed(size = OPTION_PAIR_SIZE, step = OPTION_PAIR_SIZE)
            .associate { pair ->
                require(pair.size == OPTION_PAIR_SIZE && pair.first().startsWith(OPTION_PREFIX)) {
                    usage()
                }
                pair.first() to pair.last()
            }
        require(args.size == values.size * OPTION_PAIR_SIZE) { usage() }

        val server = URI(values.required(SERVER_OPTION))
        require(server.scheme == ConnectConstants.WSS_SCHEME || isLoopbackWebSocket(server)) {
            "PocketPortal Connect requires wss:// for non-loopback servers"
        }
        val serial = values.required(SERIAL_OPTION)
        require(serial.matches(SERIAL_PATTERN)) { "Invalid Android device serial" }
        val localPort = values[LOCAL_PORT_OPTION]
            ?.toIntOrNull()
            ?: ConnectConstants.DEFAULT_LOCAL_PORT
        require(localPort in ConnectConstants.MINIMUM_PORT..ConnectConstants.MAXIMUM_PORT) {
            "Local port must be between ${ConnectConstants.MINIMUM_PORT} and " +
                "${ConnectConstants.MAXIMUM_PORT}"
        }
        val token = environment(ConnectConstants.TOKEN_ENVIRONMENT_VARIABLE)
            ?.takeIf(String::isNotBlank)
            ?: error("${ConnectConstants.TOKEN_ENVIRONMENT_VARIABLE} is required")

        return ConnectConfig(
            server = server,
            serial = serial,
            token = token,
            localPort = localPort,
            adbPath = values[ADB_PATH_OPTION] ?: ConnectConstants.DEFAULT_ADB_PATH,
            caCertificatePath = values[CA_CERTIFICATE_OPTION]?.let(Path::of),
        )
    }

    fun usage(): String =
        "Usage: pocketportal-connect --server wss://host --serial DEVICE " +
            "[--local-port ${ConnectConstants.DEFAULT_LOCAL_PORT}] [--adb adb]" +
            " [--ca-certificate /path/to/ca-bundle.pem]"

    private fun Map<String, String>.required(name: String): String =
        get(name)?.takeIf(String::isNotBlank) ?: error("Missing required option: $name")

    private fun isLoopbackWebSocket(uri: URI): Boolean =
        uri.scheme == ConnectConstants.WS_SCHEME &&
            (
                uri.host == ConnectConstants.DEFAULT_LOCAL_HOST ||
                    uri.host == ConnectConstants.LOCALHOST_NAME
            )

    private const val OPTION_PAIR_SIZE = 2
    private const val OPTION_PREFIX = "--"
    private const val SERVER_OPTION = "--server"
    private const val SERIAL_OPTION = "--serial"
    private const val LOCAL_PORT_OPTION = "--local-port"
    private const val ADB_PATH_OPTION = "--adb"
    private const val CA_CERTIFICATE_OPTION = "--ca-certificate"
    private val SERIAL_PATTERN = Regex("[A-Za-z0-9._:-]+")
}
