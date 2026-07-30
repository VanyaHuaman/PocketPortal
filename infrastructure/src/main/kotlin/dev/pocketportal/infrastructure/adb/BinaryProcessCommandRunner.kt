package dev.pocketportal.infrastructure.adb

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

internal sealed interface BinaryCommandExecution {
    data class Completed(val exitCode: Int, val bytes: ByteArray) : BinaryCommandExecution
    data object ToolNotFound : BinaryCommandExecution
    data object TimedOut : BinaryCommandExecution
    data object OutputTooLarge : BinaryCommandExecution
}

internal fun interface BinaryCommandRunner {
    fun run(command: List<String>, timeout: Duration, maximumBytes: Long): BinaryCommandExecution
}

internal class BinaryProcessCommandRunner : BinaryCommandRunner {
    override fun run(
        command: List<String>,
        timeout: Duration,
        maximumBytes: Long,
    ): BinaryCommandExecution {
        val outputFile = Files.createTempFile(TEMP_FILE_PREFIX, OUTPUT_SUFFIX)
        val errorFile = Files.createTempFile(TEMP_FILE_PREFIX, ERROR_SUFFIX)
        try {
            val process = try {
                ProcessBuilder(command)
                    .redirectInput(ProcessBuilder.Redirect.from(Path.of(NULL_DEVICE_PATH).toFile()))
                    .redirectOutput(outputFile.toFile())
                    .redirectError(errorFile.toFile())
                    .start()
            } catch (_: java.io.IOException) {
                return BinaryCommandExecution.ToolNotFound
            }

            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                process.waitFor()
                return BinaryCommandExecution.TimedOut
            }
            if (Files.size(outputFile) > maximumBytes) {
                return BinaryCommandExecution.OutputTooLarge
            }
            return BinaryCommandExecution.Completed(
                exitCode = process.exitValue(),
                bytes = Files.readAllBytes(outputFile),
            )
        } finally {
            Files.deleteIfExists(outputFile)
            Files.deleteIfExists(errorFile)
        }
    }

    private companion object {
        const val NULL_DEVICE_PATH = "/dev/null"
        const val TEMP_FILE_PREFIX = "pocketportal-screenshot-"
        const val OUTPUT_SUFFIX = ".png"
        const val ERROR_SUFFIX = ".stderr"
    }
}
