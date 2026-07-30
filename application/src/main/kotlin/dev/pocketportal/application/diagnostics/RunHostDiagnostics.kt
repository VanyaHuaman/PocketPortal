package dev.pocketportal.application.diagnostics

import dev.pocketportal.domain.diagnostics.DiagnosticReport

fun interface HostDiagnosticsGateway {
    suspend fun inspect(): DiagnosticReport
}

fun interface RunHostDiagnostics {
    suspend operator fun invoke(): DiagnosticReport
}

class RunHostDiagnosticsUseCase(
    private val gateway: HostDiagnosticsGateway,
) : RunHostDiagnostics {
    override suspend fun invoke(): DiagnosticReport = gateway.inspect()
}
