package dev.pocketportal.domain

data class SystemStatus(
    val service: String,
    val state: ServiceState,
    val observedAtEpochMillis: Long,
)

enum class ServiceState {
    READY,
}
