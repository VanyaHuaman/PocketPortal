package dev.pocketportal.connect

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = try {
        ConnectConfigParser.parse(args)
    } catch (exception: IllegalArgumentException) {
        System.err.println(exception.message)
        exitProcess(INVALID_ARGUMENT_EXIT_CODE)
    } catch (exception: IllegalStateException) {
        System.err.println(exception.message)
        exitProcess(INVALID_ARGUMENT_EXIT_CODE)
    }

    runBlocking {
        PocketPortalConnect(config).run()
    }
}

internal class PocketPortalConnect(
    private val config: ConnectConfig,
) {
    suspend fun run() {
        val endpoint = "${ConnectConstants.DEFAULT_LOCAL_HOST}:${config.localPort}"
        val shutdownHook = Thread {
            disconnectLocalAdb(endpoint)
        }
        Runtime.getRuntime().addShutdownHook(shutdownHook)
        try {
            ServerSocket(
                config.localPort,
                LOCAL_LISTENER_BACKLOG,
                InetAddress.getByName(ConnectConstants.DEFAULT_LOCAL_HOST),
            ).use { listener ->
                println("PocketPortal Connect is listening on $endpoint")
                coroutineScope {
                    val adbResult = async(Dispatchers.IO) {
                        ProcessBuilder(
                            config.adbPath,
                            ADB_CONNECT_COMMAND,
                            endpoint,
                        )
                            .inheritIO()
                            .start()
                            .waitFor()
                    }
                    val localAdb = withContext(Dispatchers.IO) { listener.accept() }
                    localAdb.use {
                        bridge(it)
                    }
                    check(adbResult.await() == SUCCESS_EXIT_CODE) {
                        "adb connect failed"
                    }
                }
            }
        } finally {
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
            withContext(Dispatchers.IO) {
                disconnectLocalAdb(endpoint)
            }
        }
    }

    private fun disconnectLocalAdb(endpoint: String) {
        runCatching {
            ProcessBuilder(config.adbPath, ADB_DISCONNECT_COMMAND, endpoint)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        }
    }

    private suspend fun bridge(localAdb: Socket) {
        val client = HttpClient(CIO) {
            engine {
                https {
                    trustManager = ClientTrust.trustManager(config.caCertificatePath)
                }
            }
            install(WebSockets)
        }
        try {
            val bridgeUri = config.server.resolve(
                "${ConnectConstants.BRIDGE_PATH_PREFIX}${config.serial}" +
                    ConnectConstants.BRIDGE_PATH_SUFFIX,
            )
            client.webSocket(
                urlString = bridgeUri.toString(),
                request = {
                    header(
                        ConnectConstants.AUTHORIZATION_HEADER,
                        ConnectConstants.BEARER_PREFIX + config.token,
                    )
                },
            ) {
                coroutineScope {
                    val clientToDevice = async(Dispatchers.IO) {
                        val buffer = ByteArray(ConnectConstants.BUFFER_BYTES)
                        while (true) {
                            val count = localAdb.getInputStream().read(buffer)
                            if (count < 0) break
                            send(Frame.Binary(fin = true, data = buffer.copyOf(count)))
                        }
                    }
                    val deviceToClient = async(Dispatchers.IO) {
                        for (frame in incoming) {
                            if (frame is Frame.Binary) {
                                localAdb.getOutputStream().write(frame.readBytes())
                                localAdb.getOutputStream().flush()
                            }
                        }
                    }
                    clientToDevice.await()
                    deviceToClient.cancel()
                }
            }
        } finally {
            client.close()
        }
    }

    private companion object {
        const val ADB_CONNECT_COMMAND = "connect"
        const val ADB_DISCONNECT_COMMAND = "disconnect"
        const val LOCAL_LISTENER_BACKLOG = 1
        const val SUCCESS_EXIT_CODE = 0
    }
}

private const val INVALID_ARGUMENT_EXIT_CODE = 2
