package dev.pocketportal.infrastructure.adb

import java.nio.file.Path
import java.nio.file.Files
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
        val standardOutputFile = Files.createTempFile(TEMP_FILE_PREFIX, STANDARD_OUTPUT_SUFFIX)
        val standardErrorFile = Files.createTempFile(TEMP_FILE_PREFIX, STANDARD_ERROR_SUFFIX)

        try {
            val process = try {
                ProcessBuilder(command)
                    .redirectInput(ProcessBuilder.Redirect.from(Path.of(NULL_DEVICE_PATH).toFile()))
                    .redirectOutput(standardOutputFile.toFile())
                    .redirectError(standardErrorFile.toFile())
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
                    standardOutput = readBounded(standardOutputFile),
                    standardError = readBounded(standardErrorFile),
                ),
            )
        } finally {
            Files.deleteIfExists(standardOutputFile)
            Files.deleteIfExists(standardErrorFile)
        }
    }

    private fun readBounded(path: Path): String =
        Files.newBufferedReader(path).use { reader ->
            val buffer = CharArray(MAX_CAPTURED_CHARACTERS)
            val count = reader.read(buffer)
            if (count < 0) "" else String(buffer, 0, count)
        }

    private companion object {
        const val NULL_DEVICE_PATH = "/dev/null"
        const val TEMP_FILE_PREFIX = "pocketportal-command-"
        const val STANDARD_OUTPUT_SUFFIX = ".stdout"
        const val STANDARD_ERROR_SUFFIX = ".stderr"
        const val MAX_CAPTURED_CHARACTERS = 65_536
    }
}
