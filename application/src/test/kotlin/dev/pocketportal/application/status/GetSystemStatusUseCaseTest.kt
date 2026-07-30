package dev.pocketportal.application.status

import dev.pocketportal.domain.ServiceState
import dev.pocketportal.domain.PocketPortalConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSystemStatusUseCaseTest {
    @Test
    fun `returns status using the supplied clock`() {
        val useCase = GetSystemStatusUseCase(clock = Clock { OBSERVED_AT })

        val result = useCase()

        assertEquals(PocketPortalConstants.SERVICE_NAME, result.service)
        assertEquals(ServiceState.READY, result.state)
        assertEquals(OBSERVED_AT, result.observedAtEpochMillis)
    }

    private companion object {
        const val OBSERVED_AT = 456L
    }
}
