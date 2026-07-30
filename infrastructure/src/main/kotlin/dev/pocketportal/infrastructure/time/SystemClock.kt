package dev.pocketportal.infrastructure.time

import dev.pocketportal.application.status.Clock

class SystemClock : Clock {
    override fun currentEpochMillis(): Long = System.currentTimeMillis()
}
