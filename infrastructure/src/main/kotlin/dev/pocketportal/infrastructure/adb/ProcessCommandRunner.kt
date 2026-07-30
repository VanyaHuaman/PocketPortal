package dev.pocketportal.infrastructure.adb

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

internal data class CommandResult(
    val exitCode: Int,
    val standardOutput: String,
    val standardError: String,
)

internal sealed interface CommandExecution {
    data class Completed(val result: CommandResult) : CommandExecution
    data object ToolNotFound : CommandExecution
    data object TimedOut : CommandExecution
}

internal fun interface CommandRunner {
    fun run(command: List<String>, timeout: Duration): CommandExecution
}

internal class ProcessCommandRunner : CommandRunner {
    override fun run(command: List<String>, timeout: Duration): CommandExecution {
        val process = try {
            ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.from(Path.of(NULL_DEVICE_PATH).toFile()))
                .start()
        } catch (_: java.io.IOException) {
            return CommandExecution.ToolNotFound
        }

        val completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (!completed) {
            process.destroyForcibly()
            process.waitFor()
            return CommandExecution.TimedOut
        }

        return CommandExecution.Completed(
            result = CommandResult(
                exitCode = process.exitValue(),
                standardOutput = process.inputStream.bufferedReader().use { it.readText() },
                standardError = process.errorStream.bufferedReader().use { it.readText() },
            ),
        )
    }

    private companion object {
        const val NULL_DEVICE_PATH = "/dev/null"
    }
}
