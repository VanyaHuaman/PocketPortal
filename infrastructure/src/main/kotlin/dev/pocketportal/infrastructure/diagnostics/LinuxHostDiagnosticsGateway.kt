package dev.pocketportal.infrastructure.diagnostics

import dev.pocketportal.application.diagnostics.HostDiagnosticsGateway
import dev.pocketportal.domain.diagnostics.DiagnosticCheck
import dev.pocketportal.domain.diagnostics.DiagnosticReport
import dev.pocketportal.domain.diagnostics.DiagnosticStatus
import dev.pocketportal.infrastructure.adb.AdbConstants
import dev.pocketportal.infrastructure.adb.AdbDeviceParser
import dev.pocketportal.infrastructure.adb.CommandExecution
import dev.pocketportal.infrastructure.adb.CommandRunner
import dev.pocketportal.infrastructure.adb.ProcessCommandRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

internal fun interface DiagnosticTextFileReader {
    fun read(path: String): String?
}

internal fun interface DiagnosticValueReader {
    fun read(name: String): String?
}

class LinuxHostDiagnosticsGateway internal constructor(
    private val adbPath: String,
    private val commandTimeout: Duration,
    private val commandRunner: CommandRunner,
    private val fileReader: DiagnosticTextFileReader,
    private val systemPropertyReader: DiagnosticValueReader,
    private val environmentReader: DiagnosticValueReader,
) : HostDiagnosticsGateway {
    constructor(adbPath: String, commandTimeout: Duration) : this(
        adbPath = adbPath,
        commandTimeout = commandTimeout,
        commandRunner = ProcessCommandRunner(),
        fileReader = DiagnosticTextFileReader { path ->
            val file = Path.of(path)
            if (Files.isRegularFile(file)) Files.readString(file) else null
        },
        systemPropertyReader = DiagnosticValueReader(System::getProperty),
        environmentReader = DiagnosticValueReader(System::getenv),
    )

    override suspend fun inspect(): DiagnosticReport = withContext(Dispatchers.IO) {
        DiagnosticReport(
            checks = listOf(
                inspectOperatingSystem(),
                inspectJava(),
                inspectAdb(),
                inspectDevices(),
                inspectSystemd(),
                inspectLinger(),
                inspectService(),
                inspectSegfaults(),
            ),
        )
    }

    private fun inspectOperatingSystem(): DiagnosticCheck {
        val values = fileReader.read(DiagnosticConstants.OS_RELEASE_PATH)
            ?.lineSequence()
            ?.mapNotNull(::parseProperty)
            ?.toMap()
            .orEmpty()
        val id = values[DiagnosticConstants.OS_ID_KEY]
        val version = values[DiagnosticConstants.OS_VERSION_KEY]

        return when {
            id != DiagnosticConstants.UBUNTU_ID ->
                fail(
                    DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID,
                    "Ubuntu is required for the supported host installer",
                    "Detected ${id ?: DiagnosticConstants.UNKNOWN_VALUE}",
                )

            version in DiagnosticConstants.SUPPORTED_UBUNTU_VERSIONS ->
                pass(
                    DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID,
                    "Ubuntu $version is supported",
                )

            else ->
                warn(
                    DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID,
                    "Ubuntu $version is outside the tested support matrix",
                    "Supported versions: ${DiagnosticConstants.SUPPORTED_UBUNTU_VERSIONS.sorted().joinToString()}",
                )
        }
    }

    private fun inspectJava(): DiagnosticCheck {
        val version = systemPropertyReader.read(DiagnosticConstants.JAVA_VERSION_PROPERTY)
            ?: DiagnosticConstants.UNKNOWN_VALUE
        val vendor = systemPropertyReader.read(DiagnosticConstants.JAVA_VENDOR_PROPERTY)
            ?: DiagnosticConstants.UNKNOWN_VALUE
        val major = version.substringBefore('.').toIntOrNull()

        return if (major != null && major >= DiagnosticConstants.MINIMUM_JAVA_VERSION) {
            pass(DiagnosticConstants.JAVA_CHECK_ID, "Java $version is available", vendor)
        } else {
            fail(
                DiagnosticConstants.JAVA_CHECK_ID,
                "Java ${DiagnosticConstants.MINIMUM_JAVA_VERSION} or newer is required",
                "Detected $version from $vendor",
            )
        }
    }

    private fun inspectAdb(): DiagnosticCheck =
        when (val execution = run(listOf(adbPath, DiagnosticConstants.ADB_VERSION_ARGUMENT))) {
            is CommandExecution.Completed ->
                if (execution.result.exitCode == DiagnosticConstants.SUCCESS_EXIT_CODE) {
                    pass(
                        DiagnosticConstants.ADB_CHECK_ID,
                        "ADB is available",
                        execution.result.standardOutput.lineSequence().firstOrNull(),
                    )
                } else {
                    fail(DiagnosticConstants.ADB_CHECK_ID, "ADB exited unsuccessfully")
                }

            CommandExecution.ToolNotFound ->
                fail(DiagnosticConstants.ADB_CHECK_ID, "ADB was not found", adbPath)

            CommandExecution.TimedOut ->
                fail(DiagnosticConstants.ADB_CHECK_ID, "ADB version check timed out")
        }

    private fun inspectDevices(): DiagnosticCheck =
        when (
            val execution = run(
                listOf(
                    adbPath,
                    AdbConstants.DEVICES_COMMAND,
                    AdbConstants.LONG_LISTING_FLAG,
                ),
            )
        ) {
            is CommandExecution.Completed -> {
                if (execution.result.exitCode != DiagnosticConstants.SUCCESS_EXIT_CODE) {
                    fail(DiagnosticConstants.DEVICES_CHECK_ID, "ADB device discovery failed")
                } else {
                    val devices = AdbDeviceParser.parse(execution.result.standardOutput)
                    if (devices.isEmpty()) {
                        warn(DiagnosticConstants.DEVICES_CHECK_ID, "No Android devices are visible")
                    } else {
                        pass(
                            DiagnosticConstants.DEVICES_CHECK_ID,
                            "${devices.size} Android device(s) visible",
                            devices.joinToString { "${it.serial.value}:${it.state.name.lowercase()}" },
                        )
                    }
                }
            }

            CommandExecution.ToolNotFound ->
                fail(DiagnosticConstants.DEVICES_CHECK_ID, "Device discovery requires ADB")

            CommandExecution.TimedOut ->
                fail(DiagnosticConstants.DEVICES_CHECK_ID, "ADB device discovery timed out")
        }

    private fun inspectSystemd(): DiagnosticCheck {
        val execution = run(
            listOf(
                DiagnosticConstants.SYSTEMCTL_COMMAND,
                DiagnosticConstants.USER_ARGUMENT,
                DiagnosticConstants.SYSTEMD_STATE_ARGUMENT,
            ),
        )
        val state = (execution as? CommandExecution.Completed)
            ?.result
            ?.standardOutput
            ?.trim()

        return if (
            state == DiagnosticConstants.SYSTEMD_RUNNING ||
            state == DiagnosticConstants.SYSTEMD_DEGRADED
        ) {
            pass(DiagnosticConstants.SYSTEMD_CHECK_ID, "User systemd is available", state)
        } else {
            fail(
                DiagnosticConstants.SYSTEMD_CHECK_ID,
                "User systemd is not available",
                state,
            )
        }
    }

    private fun inspectLinger(): DiagnosticCheck {
        val user = environmentReader.read(DiagnosticConstants.USER_ENVIRONMENT_VARIABLE)
            ?: return fail(
                DiagnosticConstants.LINGER_CHECK_ID,
                "Cannot determine the current user",
            )
        val execution = run(
            listOf(
                DiagnosticConstants.LOGINCTL_COMMAND,
                DiagnosticConstants.SHOW_USER_ARGUMENT,
                user,
                DiagnosticConstants.PROPERTY_ARGUMENT,
                DiagnosticConstants.LINGER_PROPERTY,
                DiagnosticConstants.VALUE_ARGUMENT,
            ),
        )
        val value = (execution as? CommandExecution.Completed)
            ?.result
            ?.standardOutput
            ?.trim()

        return if (value == DiagnosticConstants.LINGER_ENABLED) {
            pass(DiagnosticConstants.LINGER_CHECK_ID, "Systemd lingering is enabled")
        } else {
            warn(
                DiagnosticConstants.LINGER_CHECK_ID,
                "Systemd lingering is not enabled",
                "Run: sudo loginctl enable-linger $user",
            )
        }
    }

    private fun inspectService(): DiagnosticCheck {
        val execution = run(
            listOf(
                DiagnosticConstants.SYSTEMCTL_COMMAND,
                DiagnosticConstants.USER_ARGUMENT,
                DiagnosticConstants.SERVICE_STATE_ARGUMENT,
                DiagnosticConstants.POCKETPORTAL_SERVICE,
            ),
        )
        val value = (execution as? CommandExecution.Completed)
            ?.result
            ?.standardOutput
            ?.trim()

        return if (value == DiagnosticConstants.SERVICE_ACTIVE) {
            pass(DiagnosticConstants.SERVICE_CHECK_ID, "PocketPortal service is active")
        } else {
            warn(
                DiagnosticConstants.SERVICE_CHECK_ID,
                "PocketPortal service is not active",
                value,
            )
        }
    }

    private fun inspectSegfaults(): DiagnosticCheck {
        val execution = run(
            listOf(
                DiagnosticConstants.JOURNALCTL_COMMAND,
                DiagnosticConstants.KERNEL_ARGUMENT,
                DiagnosticConstants.CURRENT_BOOT_ARGUMENT,
                DiagnosticConstants.NO_PAGER_ARGUMENT,
            ),
        )
        val count = (execution as? CommandExecution.Completed)
            ?.result
            ?.standardOutput
            ?.lineSequence()
            ?.count { it.contains(DiagnosticConstants.SEGFAULT_MARKER, ignoreCase = true) }
            ?: return warn(
                DiagnosticConstants.SEGFAULT_CHECK_ID,
                "Kernel stability history could not be inspected",
            )

        return if (count == DiagnosticConstants.NO_SEGFAULTS) {
            pass(DiagnosticConstants.SEGFAULT_CHECK_ID, "No process segfaults recorded this boot")
        } else {
            warn(
                DiagnosticConstants.SEGFAULT_CHECK_ID,
                "$count process segfault(s) recorded this boot",
                "Investigate host stability before unattended operation",
            )
        }
    }

    private fun run(command: List<String>): CommandExecution =
        commandRunner.run(command, commandTimeout)

    private fun parseProperty(line: String): Pair<String, String>? {
        val separator = line.indexOf('=')
        if (separator <= 0) return null
        return line.substring(0, separator) to
            line.substring(separator + PROPERTY_VALUE_OFFSET).trim().trim('"')
    }

    private fun pass(id: String, summary: String, detail: String? = null) =
        DiagnosticCheck(id, DiagnosticStatus.PASS, summary, detail)

    private fun warn(id: String, summary: String, detail: String? = null) =
        DiagnosticCheck(id, DiagnosticStatus.WARN, summary, detail)

    private fun fail(id: String, summary: String, detail: String? = null) =
        DiagnosticCheck(id, DiagnosticStatus.FAIL, summary, detail)

    private companion object {
        const val PROPERTY_VALUE_OFFSET = 1
    }
}
