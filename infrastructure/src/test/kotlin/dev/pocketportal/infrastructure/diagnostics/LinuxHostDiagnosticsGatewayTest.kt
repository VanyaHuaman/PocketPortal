package dev.pocketportal.infrastructure.diagnostics

import dev.pocketportal.domain.diagnostics.DiagnosticStatus
import dev.pocketportal.infrastructure.adb.CommandExecution
import dev.pocketportal.infrastructure.adb.CommandResult
import dev.pocketportal.infrastructure.adb.CommandRunner
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinuxHostDiagnosticsGatewayTest {
    @Test
    fun `reports healthy prerequisites while warning about an end-of-life host`() = runTest {
        val gateway = gateway(
            osRelease = "ID=ubuntu\nVERSION_ID=\"25.10\"\nPRETTY_NAME=\"Ubuntu 25.10\"\n",
            outputs = healthyOutputs(),
        )

        val report = gateway.inspect()

        assertFalse(report.hasFailures)
        assertEquals(
            DiagnosticStatus.WARN,
            report.checks.single { it.id == DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID }.status,
        )
        assertTrue(
            report.checks
                .filterNot { it.id == DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID }
                .all { it.status == DiagnosticStatus.PASS },
        )
        assertEquals(DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID, report.checks.first().id)
    }

    @Test
    fun `distinguishes unsupported host warnings from required tool failures`() = runTest {
        val outputs = healthyOutputs().toMutableMap().apply {
            this[ADB_VERSION_COMMAND] = CommandExecution.ToolNotFound
            this[ADB_DEVICES_COMMAND] = CommandExecution.ToolNotFound
        }
        val gateway = gateway(
            osRelease = "ID=fedora\nVERSION_ID=\"44\"\nPRETTY_NAME=\"Fedora Linux 44\"\n",
            outputs = outputs,
        )

        val report = gateway.inspect()

        assertTrue(report.hasFailures)
        assertEquals(
            DiagnosticStatus.WARN,
            report.checks.single { it.id == DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID }.status,
        )
        assertEquals(
            DiagnosticStatus.FAIL,
            report.checks.single { it.id == DiagnosticConstants.ADB_CHECK_ID }.status,
        )
    }

    @Test
    fun `accepts an unknown Linux distribution with a warning`() = runTest {
        val gateway = gateway(
            osRelease = "ID=examplelinux\nVERSION_ID=\"1\"\nPRETTY_NAME=\"Example Linux 1\"\n",
            outputs = healthyOutputs(),
        )

        val report = gateway.inspect()

        assertFalse(report.hasFailures)
        assertEquals(
            DiagnosticStatus.WARN,
            report.checks.single { it.id == DiagnosticConstants.OPERATING_SYSTEM_CHECK_ID }.status,
        )
    }

    @Test
    fun `warns when the current boot contains segfaults`() = runTest {
        val outputs = healthyOutputs().toMutableMap().apply {
            this[JOURNAL_COMMAND] = completed("kernel: process segfault at 0\n")
        }
        val gateway = gateway(outputs = outputs)

        val report = gateway.inspect()

        val check = report.checks.single { it.id == DiagnosticConstants.SEGFAULT_CHECK_ID }
        assertEquals(DiagnosticStatus.WARN, check.status)
        assertTrue(check.summary.startsWith("1 process"))
    }

    private fun gateway(
        osRelease: String =
            "ID=ubuntu\nVERSION_ID=\"25.10\"\nPRETTY_NAME=\"Ubuntu 25.10\"\n",
        outputs: Map<List<String>, CommandExecution>,
    ) = LinuxHostDiagnosticsGateway(
        adbPath = ADB_PATH,
        commandTimeout = Duration.ofSeconds(1),
        commandRunner = CommandRunner { command, _ ->
            outputs[command] ?: error("Unexpected command: $command")
        },
        fileReader = DiagnosticTextFileReader { osRelease },
        systemPropertyReader = DiagnosticValueReader { name ->
            when (name) {
                DiagnosticConstants.JAVA_VERSION_PROPERTY -> "21.0.8"
                DiagnosticConstants.JAVA_VENDOR_PROPERTY -> "Test Vendor"
                else -> null
            }
        },
        environmentReader = DiagnosticValueReader { name ->
            if (name == DiagnosticConstants.USER_ENVIRONMENT_VARIABLE) TEST_USER else null
        },
    )

    private fun healthyOutputs(): Map<List<String>, CommandExecution> = mapOf(
        ADB_VERSION_COMMAND to completed("Android Debug Bridge version 1.0.41\n"),
        ADB_DEVICES_COMMAND to completed(
            "List of devices attached\n$DEVICE_SERIAL\tdevice product:test model:Pixel\n",
        ),
        SYSTEMD_COMMAND to completed("running\n"),
        LINGER_COMMAND to completed("yes\n"),
        SERVICE_COMMAND to completed("active\n"),
        JOURNAL_COMMAND to completed("kernel: boot complete\n"),
    )

    private fun completed(output: String) = CommandExecution.Completed(
        CommandResult(
            exitCode = DiagnosticConstants.SUCCESS_EXIT_CODE,
            standardOutput = output,
            standardError = "",
        ),
    )

    private companion object {
        const val ADB_PATH = "/usr/bin/adb"
        const val TEST_USER = "tester"
        const val DEVICE_SERIAL = "ABC123"

        val ADB_VERSION_COMMAND = listOf(ADB_PATH, DiagnosticConstants.ADB_VERSION_ARGUMENT)
        val ADB_DEVICES_COMMAND = listOf(ADB_PATH, "devices", "-l")
        val SYSTEMD_COMMAND = listOf("systemctl", "--user", "is-system-running")
        val LINGER_COMMAND = listOf(
            "loginctl",
            "show-user",
            TEST_USER,
            "-p",
            "Linger",
            "--value",
        )
        val SERVICE_COMMAND = listOf("systemctl", "--user", "is-active", "pocketportal.service")
        val JOURNAL_COMMAND = listOf("journalctl", "-k", "-b", "--no-pager")
    }
}
