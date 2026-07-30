package dev.pocketportal.domain.diagnostics

data class DiagnosticReport(
    val checks: List<DiagnosticCheck>,
) {
    val hasFailures: Boolean
        get() = checks.any { it.status == DiagnosticStatus.FAIL }
}

data class DiagnosticCheck(
    val id: String,
    val status: DiagnosticStatus,
    val summary: String,
    val detail: String? = null,
)

enum class DiagnosticStatus {
    PASS,
    WARN,
    FAIL,
}
