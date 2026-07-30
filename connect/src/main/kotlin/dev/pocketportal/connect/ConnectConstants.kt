package dev.pocketportal.connect

internal object ConnectConstants {
    const val TOKEN_ENVIRONMENT_VARIABLE = "POCKETPORTAL_CONNECT_TOKEN"
    const val DEFAULT_LOCAL_HOST = "127.0.0.1"
    const val DEFAULT_LOCAL_PORT = 15_556
    const val DEFAULT_ADB_PATH = "adb"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer "
    const val BRIDGE_PATH_PREFIX = "/api/devices/"
    const val BRIDGE_PATH_SUFFIX = "/adb"
    const val BUFFER_BYTES = 65_536
    const val MINIMUM_PORT = 1
    const val MAXIMUM_PORT = 65_535
    const val WSS_SCHEME = "wss"
    const val WS_SCHEME = "ws"
    const val LOCALHOST_NAME = "localhost"
}
