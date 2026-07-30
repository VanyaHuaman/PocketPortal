package dev.pocketportal.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SystemStatusTest {
    @Test
    fun `represents a ready PocketPortal service`() {
        val status = SystemStatus(
            service = PocketPortalConstants.SERVICE_NAME,
            state = ServiceState.READY,
            observedAtEpochMillis = OBSERVED_AT,
        )

        assertEquals(PocketPortalConstants.SERVICE_NAME, status.service)
        assertEquals(ServiceState.READY, status.state)
        assertEquals(OBSERVED_AT, status.observedAtEpochMillis)
    }

    private companion object {
        const val OBSERVED_AT = 123L
    }
}
