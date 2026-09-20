package com.prime.nobuffer.blocklist

import com.prime.nobuffer.data.entity.BlockedSite
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure lock-policy evaluation. No Android types — unit-testable on the JVM.
 *
 * Schedule days: bit0=Monday … bit6=Sunday. [isoDayOfWeek] is ISO-8601 (1=Mon … 7=Sun).
 * Schedule window is half-open `[start, end)` in minutes from midnight; `start > end` wraps
 * past midnight (e.g. 22:00–07:00). Equal start/end is treated as an all-day window.
 */
object SiteLockPolicy {

    const val MINUTES_PER_DAY = 1440
    const val DEFAULT_DAYS_MASK = 0b1111111

    fun isActivelyLocked(
        site: BlockedSite,
        nowMillis: Long,
        nowMinuteOfDay: Int,
        isoDayOfWeek: Int
    ): Boolean {
        if (isBudgetExhausted(site, nowMillis)) return true
        if (site.unlockUntil > nowMillis) return false
        if (isScheduleSet(site)) {
            if (!dayInMask(site.scheduleDaysMask, isoDayOfWeek)) return false
            if (!scheduleContains(site.scheduleStartMinute, site.scheduleEndMinute, nowMinuteOfDay)) {
                return false
            }
            return true
        }
        return true
    }

    fun isActivelyLocked(site: BlockedSite, nowMillis: Long = System.currentTimeMillis()): Boolean =
        isActivelyLocked(site, nowMillis, minuteOfDay(nowMillis), isoDayOfWeek(nowMillis))

    fun isScheduleSet(site: BlockedSite): Boolean =
        site.scheduleStartMinute != -1 && site.scheduleEndMinute != -1

    fun dayInMask(mask: Int, isoDayOfWeek: Int): Boolean {
        if (isoDayOfWeek !in 1..7) return false
        return mask and (1 shl (isoDayOfWeek - 1)) != 0
    }

    fun scheduleContains(start: Int, end: Int, nowMinute: Int): Boolean {
        if (start !in 0 until MINUTES_PER_DAY || end !in 0 until MINUTES_PER_DAY) return false
        if (nowMinute !in 0 until MINUTES_PER_DAY) return false
        return when {
            start == end -> true
            start < end -> nowMinute in start until end
            else -> nowMinute >= start || nowMinute < end
        }
    }

    fun epochDay(nowMillis: Long): Long =
        zoned(nowMillis).toLocalDate().toEpochDay()

    fun minuteOfDay(nowMillis: Long): Int {
        val time = zoned(nowMillis).toLocalTime()
        return time.hour * 60 + time.minute
    }

    fun isoDayOfWeek(nowMillis: Long): Int = zoned(nowMillis).dayOfWeek.value

    fun budgetCapMillis(dailyBudgetMinutes: Int): Long =
        if (dailyBudgetMinutes <= 0) 0L else dailyBudgetMinutes * 60_000L

    fun usedMillisForToday(site: BlockedSite, nowMillis: Long): Long {
        val day = epochDay(nowMillis)
        return if (site.budgetDayEpoch == day) site.usedMillisToday else 0L
    }

    fun remainingBudgetMillis(site: BlockedSite, nowMillis: Long): Long? {
        if (site.dailyBudgetMinutes <= 0) return null
        return (budgetCapMillis(site.dailyBudgetMinutes) - usedMillisForToday(site, nowMillis))
            .coerceAtLeast(0L)
    }

    fun isBudgetExhausted(site: BlockedSite, nowMillis: Long): Boolean {
        val remaining = remainingBudgetMillis(site, nowMillis) ?: return false
        return remaining <= 0L
    }

    fun parseMinuteOfDay(text: String): Int? {
        val s = text.trim()
        val parts = s.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun formatMinuteOfDay(minute: Int): String {
        if (minute !in 0 until MINUTES_PER_DAY) return ""
        return "%02d:%02d".format(minute / 60, minute % 60)
    }

    private fun zoned(nowMillis: Long): ZonedDateTime =
        Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
}
