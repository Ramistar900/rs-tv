package com.sinjartv.rstv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RamiProgrammeScheduleStoreTest {
    private fun entry(
        id: String,
        channelId: Int = 1,
        minute: Int,
        days: Set<Int>,
        durationMinutes: Long
    ) = RamiProgrammeSchedule(
        id = id,
        channelId = channelId,
        videoUri = "content://media/$id",
        startMinute = minute,
        weekdays = days,
        durationMs = durationMinutes * 60_000L
    )

    @Test
    fun rejectsOverlappingProgramsOnSameDay() {
        val first = entry("first", minute = 18 * 60, days = setOf(1), durationMinutes = 90)
        val next = entry("next", minute = 19 * 60, days = setOf(1), durationMinutes = 30)
        assertTrue(RamiProgrammeScheduleStore.hasConflict(next, listOf(first)))
    }

    @Test
    fun allowsProgramsWhenFirstVideoEndsBeforeNextStart() {
        val first = entry("first", minute = 18 * 60, days = setOf(1), durationMinutes = 45)
        val next = entry("next", minute = 19 * 60, days = setOf(1), durationMinutes = 30)
        assertFalse(RamiProgrammeScheduleStore.hasConflict(next, listOf(first)))
    }

    @Test
    fun detectsOverlapAcrossMidnightAndWeekBoundary() {
        val saturday = entry("late-saturday", minute = 23 * 60, days = setOf(7), durationMinutes = 120)
        val sunday = entry("early-sunday", minute = 30, days = setOf(1), durationMinutes = 30)
        assertTrue(RamiProgrammeScheduleStore.hasConflict(sunday, listOf(saturday)))
    }

    @Test
    fun schedulesOnDifferentChannelsDoNotConflict() {
        val first = entry("first", channelId = 1, minute = 18 * 60, days = setOf(1), durationMinutes = 120)
        val otherChannel = entry("other", channelId = 2, minute = 18 * 60, days = setOf(1), durationMinutes = 120)
        assertFalse(RamiProgrammeScheduleStore.hasConflict(otherChannel, listOf(first)))
    }

    @Test
    fun editingDoesNotConflictWithItself() {
        val existing = entry("same", minute = 18 * 60, days = setOf(1), durationMinutes = 60)
        val edited = existing.copy(startMinute = 18 * 30)
        assertFalse(RamiProgrammeScheduleStore.hasConflict(edited, listOf(existing), excludingId = "same"))
    }
}
