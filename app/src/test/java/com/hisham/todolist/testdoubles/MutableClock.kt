package com.hisham.todolist.testdoubles

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class MutableClock(
    var instant: Instant,
    private val zone: ZoneId,
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration)
    }
}
