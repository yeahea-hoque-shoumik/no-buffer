package com.prime.nobuffer.blocklist

import com.prime.nobuffer.data.entity.BlockedSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SiteLockPolicyTest {

    private val now = System.currentTimeMillis()
    private val minute = SiteLockPolicy.minuteOfDay(now)
    private val dow = SiteLockPolicy.isoDayOfWeek(now)

    @Test
    fun timedUnlockWindowNotLocked() {
        val site = BlockedSite(
            host = "youtube.com",
            unlockUntil = now + 15 * 60_000L
        )
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
        val expired = site.copy(unlockUntil = now - 1)
        assertTrue(SiteLockPolicy.isActivelyLocked(expired, now, minute, dow))
    }

    @Test
    fun noScheduleAlwaysLockedUnlessTimedUnlock() {
        val locked = BlockedSite(host = "reddit.com")
        assertTrue(SiteLockPolicy.isActivelyLocked(locked, now, minute, dow))
        val timed = locked.copy(unlockUntil = now + 5_000L)
        assertFalse(SiteLockPolicy.isActivelyLocked(timed, now, minute, dow))
    }

    @Test
    fun weekdayOvernightScheduleWrapsPastMidnight() {
        // Mon–Fri 22:00–07:00
        val weekdays = 0b0011111
        val site = BlockedSite(
            host = "tiktok.com",
            scheduleStartMinute = 22 * 60,
            scheduleEndMinute = 7 * 60,
            scheduleDaysMask = weekdays
        )
        val monday = 1
        val saturday = 6

        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, 22 * 60, monday))
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, 23 * 60 + 30, monday))
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, 0, monday))
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, 6 * 60 + 59, monday))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 7 * 60, monday))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 12 * 60, monday))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 21 * 60 + 59, monday))

        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 23 * 60, saturday))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 2 * 60, saturday))
    }

    @Test
    fun dayMaskIgnoresUnselectedDays() {
        val monOnly = 0b0000001
        val site = BlockedSite(
            host = "x.com",
            scheduleStartMinute = 9 * 60,
            scheduleEndMinute = 17 * 60,
            scheduleDaysMask = monOnly
        )
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, 10 * 60, isoDayOfWeek = 1))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 10 * 60, isoDayOfWeek = 2))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, 10 * 60, isoDayOfWeek = 7))
    }

    @Test
    fun timedUnlockOverridesScheduleWindow() {
        val site = BlockedSite(
            host = "netflix.com",
            unlockUntil = now + 60_000L,
            scheduleStartMinute = 0,
            scheduleEndMinute = 23 * 60 + 59,
            scheduleDaysMask = SiteLockPolicy.DEFAULT_DAYS_MASK
        )
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
    }

    @Test
    fun budgetExhaustionLocksEvenDuringTimedUnlock() {
        val site = BlockedSite(
            host = "youtube.com",
            unlockUntil = now + 30 * 60_000L,
            dailyBudgetMinutes = 15,
            usedMillisToday = 15 * 60_000L,
            budgetDayEpoch = SiteLockPolicy.epochDay(now)
        )
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
        assertTrue(SiteLockPolicy.isBudgetExhausted(site, now))
    }

    @Test
    fun budgetResetsWhenEpochDayChanges() {
        val site = BlockedSite(
            host = "youtube.com",
            unlockUntil = now + 10_000L,
            dailyBudgetMinutes = 15,
            usedMillisToday = 15 * 60_000L,
            budgetDayEpoch = SiteLockPolicy.epochDay(now) - 1
        )
        assertFalse(SiteLockPolicy.isBudgetExhausted(site, now))
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
    }

    @Test
    fun unusedBudgetDoesNotLock() {
        val site = BlockedSite(
            host = "youtube.com",
            unlockUntil = now + 10_000L,
            dailyBudgetMinutes = 30,
            usedMillisToday = 5 * 60_000L,
            budgetDayEpoch = SiteLockPolicy.epochDay(now)
        )
        assertFalse(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
        assertEquals(25 * 60_000L, SiteLockPolicy.remainingBudgetMillis(site, now))
    }

    @Test
    fun videoAllowedDoesNotUnlockNavigation() {
        val site = BlockedSite(
            host = "youtube.com",
            videoAllowed = true
        )
        assertTrue(SiteLockPolicy.isActivelyLocked(site, now, minute, dow))
    }

    @Test
    fun scheduleContainsNonWrapHalfOpen() {
        assertTrue(SiteLockPolicy.scheduleContains(9 * 60, 17 * 60, 9 * 60))
        assertTrue(SiteLockPolicy.scheduleContains(9 * 60, 17 * 60, 16 * 60 + 59))
        assertFalse(SiteLockPolicy.scheduleContains(9 * 60, 17 * 60, 17 * 60))
        assertFalse(SiteLockPolicy.scheduleContains(9 * 60, 17 * 60, 8 * 60 + 59))
    }

    @Test
    fun scheduleContainsOvernightWrap() {
        val start = 22 * 60
        val end = 7 * 60
        assertTrue(SiteLockPolicy.scheduleContains(start, end, 22 * 60))
        assertTrue(SiteLockPolicy.scheduleContains(start, end, 0))
        assertTrue(SiteLockPolicy.scheduleContains(start, end, 7 * 60 - 1))
        assertFalse(SiteLockPolicy.scheduleContains(start, end, 7 * 60))
        assertFalse(SiteLockPolicy.scheduleContains(start, end, 21 * 60 + 59))
        assertFalse(SiteLockPolicy.scheduleContains(start, end, 12 * 60))
    }

    @Test
    fun clockHelpersMatchDefaultZone() {
        val zoned = ZonedDateTime.of(
            LocalDate.of(2026, 3, 9), // Monday
            LocalTime.of(22, 30),
            ZoneId.systemDefault()
        )
        val millis = zoned.toInstant().toEpochMilli()
        assertEquals(zoned.toLocalDate().toEpochDay(), SiteLockPolicy.epochDay(millis))
        assertEquals(22 * 60 + 30, SiteLockPolicy.minuteOfDay(millis))
        assertEquals(1, SiteLockPolicy.isoDayOfWeek(millis))
    }

    @Test
    fun parseAndFormatMinuteOfDay() {
        assertEquals(22 * 60, SiteLockPolicy.parseMinuteOfDay("22:00"))
        assertEquals(7 * 60, SiteLockPolicy.parseMinuteOfDay("07:00"))
        assertEquals(9 * 60 + 5, SiteLockPolicy.parseMinuteOfDay("9:05"))
        assertNull(SiteLockPolicy.parseMinuteOfDay("25:00"))
        assertNull(SiteLockPolicy.parseMinuteOfDay("abc"))
        assertEquals("22:00", SiteLockPolicy.formatMinuteOfDay(22 * 60))
        assertEquals("07:05", SiteLockPolicy.formatMinuteOfDay(7 * 60 + 5))
    }

    @Test
    fun dayInMaskBits() {
        val weekdays = 0b0011111
        assertTrue(SiteLockPolicy.dayInMask(weekdays, 1))
        assertTrue(SiteLockPolicy.dayInMask(weekdays, 5))
        assertFalse(SiteLockPolicy.dayInMask(weekdays, 6))
        assertFalse(SiteLockPolicy.dayInMask(weekdays, 7))
        assertTrue(SiteLockPolicy.dayInMask(0b1000000, 7))
    }
}
