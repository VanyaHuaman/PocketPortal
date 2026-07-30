package dev.pocketportal.application.diagnostics

import dev.pocketportal.domain.diagnostics.DiagnosticCheck
import dev.pocketportal.domain.diagnostics.DiagnosticReport
import dev.pocketportal.domain.diagnostics.DiagnosticStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RunHostDiagnosticsUseCaseTest {
    @Test
    fun `returns the gateway report unchanged`() = runTest {
        val expected = DiagnosticReport(
            checks = listOf(
                DiagnosticCheck(
                    id = CHECK_ID,
                    status = DiagnosticStatus.PASS,
                    summary = CHECK_SUMMARY,
                ),
            ),
        )
        val useCase = RunHostDiagnosticsUseCase(
            gateway = HostDiagnosticsGateway { expected },
        )

        assertEquals(expected, useCase())
    }

    private companion object {
        const val CHECK_ID = "test"
        const val CHECK_SUMMARY = "Test passed"
    }
}
