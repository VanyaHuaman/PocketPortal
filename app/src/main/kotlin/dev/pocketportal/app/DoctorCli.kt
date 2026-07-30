package dev.pocketportal.app

import dev.pocketportal.application.diagnostics.RunHostDiagnostics
import dev.pocketportal.domain.diagnostics.DiagnosticStatus
import kotlinx.coroutines.runBlocking

object CliConstants {
    const val DOCTOR_COMMAND = "doctor"
    const val SUCCESS_EXIT_CODE = 0
    const val FAILURE_EXIT_CODE = 1
    const val STATUS_WIDTH = 4
    const val PASS_MARKER = "PASS"
    const val WARN_MARKER = "WARN"
    const val FAIL_MARKER = "FAIL"
}

fun runDoctor(runHostDiagnostics: RunHostDiagnostics): Int = runBlocking {
    val report = runHostDiagnostics()

    report.checks.forEach { check ->
        val marker = when (check.status) {
            DiagnosticStatus.PASS -> CliConstants.PASS_MARKER
            DiagnosticStatus.WARN -> CliConstants.WARN_MARKER
            DiagnosticStatus.FAIL -> CliConstants.FAIL_MARKER
        }
        println("${marker.padEnd(CliConstants.STATUS_WIDTH)}  ${check.summary}")
        check.detail?.let { println("      $it") }
    }

    if (report.hasFailures) CliConstants.FAILURE_EXIT_CODE else CliConstants.SUCCESS_EXIT_CODE
}
