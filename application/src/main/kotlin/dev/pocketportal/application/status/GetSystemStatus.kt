package dev.pocketportal.application.status

import dev.pocketportal.domain.ServiceState
import dev.pocketportal.domain.SystemStatus
import dev.pocketportal.domain.PocketPortalConstants

fun interface Clock {
    fun currentEpochMillis(): Long
}

fun interface GetSystemStatus {
    operator fun invoke(): SystemStatus
}

class GetSystemStatusUseCase(
    private val clock: Clock,
) : GetSystemStatus {
    override fun invoke(): SystemStatus = SystemStatus(
        service = PocketPortalConstants.SERVICE_NAME,
        state = ServiceState.READY,
        observedAtEpochMillis = clock.currentEpochMillis(),
    )
}
