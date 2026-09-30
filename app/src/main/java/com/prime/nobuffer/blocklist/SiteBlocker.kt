package com.prime.nobuffer.blocklist

import com.prime.nobuffer.data.BrowserRepository
import com.prime.nobuffer.data.entity.BlockedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SiteBlocker(
    private val repository: BrowserRepository,
    val passwordStore: LockPasswordStore,
    scope: CoroutineScope,
    private val gambling: GamblingBlocklist? = null
) {
    private val _sites = MutableStateFlow<List<BlockedSite>>(emptyList())
    val sites: StateFlow<List<BlockedSite>> = _sites.asStateFlow()

    private val _blockedHosts = MutableStateFlow<Set<String>>(emptySet())
    val blockedHosts: StateFlow<Set<String>> = _blockedHosts.asStateFlow()

    val hasPassword: StateFlow<Boolean> = passwordStore.hasPassword
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        runBlocking(Dispatchers.IO) {
            replaceSnapshot(repository.getBlockedSites())
        }
        scope.launch {
            repository.observeBlockedSites().collect { list ->
                replaceSnapshot(list)
            }
        }
    }

    /** True for forced gambling blocks, which have no unlock path (unlike user-added sites). */
    fun isGamblingUrl(url: String): Boolean = gambling?.isBlocked(url) == true

    fun isUrlBlocked(url: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isGamblingUrl(url)) return true
        val site = matchingSite(url) ?: return false
        return SiteLockPolicy.isActivelyLocked(site, nowMillis)
    }

    fun isVideoAllowed(url: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val site = matchingSite(url) ?: return false
        if (SiteLockPolicy.isActivelyLocked(site, nowMillis)) return false
        return site.videoAllowed
    }

    fun matchingSite(urlOrHost: String): BlockedSite? {
        val host = BlockedHostMatcher.hostFromUrl(urlOrHost) ?: return null
        val list = _sites.value
        if (list.isEmpty()) return null
        var current = host
        while (true) {
            list.find { it.host == current }?.let { return it }
            val dot = current.indexOf('.')
            if (dot <= 0) return null
            current = current.substring(dot + 1)
        }
    }

    suspend fun addSite(raw: String): String? {
        if (!passwordStore.hasPasswordNow()) {
            return "Set a lock password before blocking sites"
        }
        val host = BlockedHostMatcher.normalizeHost(raw)
            ?: return "Enter a domain like youtube.com"
        if (_sites.value.any { it.host == host }) return null
        val site = BlockedSite(host = host)
        applyLocal(site)
        repository.upsertBlockedSite(site)
        return null
    }

    suspend fun unlockSite(hostOrUrl: String, password: String): String? {
        BlockedHostMatcher.normalizeHost(hostOrUrl) ?: return "Invalid site"
        val site = matchingSite(hostOrUrl) ?: return "Site is not blocked"
        requirePassword(password)?.let { return it }
        removeLocal(site.host)
        repository.deleteBlockedSite(site.host)
        return null
    }

    suspend fun unlockAll(password: String): String? {
        requirePassword(password)?.let { return it }
        replaceSnapshot(emptyList())
        repository.clearBlockedSites()
        return null
    }

    suspend fun timedUnlock(hostOrUrl: String, password: String, durationMillis: Long): String? {
        if (durationMillis <= 0L) return "Duration must be positive"
        val site = matchingSite(hostOrUrl) ?: return "Site is not blocked"
        requirePassword(password)?.let { return it }
        val now = System.currentTimeMillis()
        if (SiteLockPolicy.isBudgetExhausted(site, now)) {
            return "Daily time budget is used up"
        }
        val updated = site.copy(unlockUntil = now + durationMillis)
        applyLocal(updated)
        repository.updateBlockedSiteUnlockUntil(updated.host, updated.unlockUntil)
        return null
    }

    suspend fun quickLock(hostOrUrl: String): String? {
        val site = matchingSite(hostOrUrl) ?: return "Site is not blocked"
        val updated = site.copy(unlockUntil = 0L)
        applyLocal(updated)
        repository.updateBlockedSiteUnlockUntil(updated.host, 0L)
        return null
    }

    suspend fun setSchedule(
        host: String,
        startMinute: Int,
        endMinute: Int,
        daysMask: Int,
        password: String
    ): String? {
        val site = matchingSite(host) ?: return "Site is not blocked"
        val clearing = startMinute == -1 && endMinute == -1
        if (!clearing && (startMinute !in 0 until SiteLockPolicy.MINUTES_PER_DAY ||
                endMinute !in 0 until SiteLockPolicy.MINUTES_PER_DAY)
        ) {
            return "Enter times like 22:00 and 07:00"
        }
        requirePassword(password)?.let { return it }
        val mask = daysMask and SiteLockPolicy.DEFAULT_DAYS_MASK
        val updated = site.copy(
            scheduleStartMinute = if (clearing) -1 else startMinute,
            scheduleEndMinute = if (clearing) -1 else endMinute,
            scheduleDaysMask = if (clearing) SiteLockPolicy.DEFAULT_DAYS_MASK else mask
        )
        applyLocal(updated)
        repository.updateBlockedSiteSchedule(
            updated.host,
            updated.scheduleStartMinute,
            updated.scheduleEndMinute,
            updated.scheduleDaysMask
        )
        return null
    }

    suspend fun setDailyBudget(host: String, minutes: Int, password: String): String? {
        if (minutes < 0) return "Budget must be 0 or more minutes"
        val site = matchingSite(host) ?: return "Site is not blocked"
        requirePassword(password)?.let { return it }
        val now = System.currentTimeMillis()
        val updated = site.copy(
            dailyBudgetMinutes = minutes,
            usedMillisToday = 0L,
            budgetDayEpoch = SiteLockPolicy.epochDay(now)
        )
        applyLocal(updated)
        repository.updateBlockedSiteDailyBudget(
            updated.host,
            updated.dailyBudgetMinutes,
            updated.usedMillisToday,
            updated.budgetDayEpoch
        )
        return null
    }

    suspend fun setVideoAllowed(host: String, allowed: Boolean, password: String): String? {
        val site = matchingSite(host) ?: return "Site is not blocked"
        requirePassword(password)?.let { return it }
        val updated = site.copy(videoAllowed = allowed)
        applyLocal(updated)
        repository.updateBlockedSiteVideoAllowed(updated.host, allowed)
        return null
    }

    suspend fun recordUsage(hostOrUrl: String, elapsedMillis: Long) {
        if (elapsedMillis <= 0L) return
        val site = matchingSite(hostOrUrl) ?: return
        val now = System.currentTimeMillis()
        val day = SiteLockPolicy.epochDay(now)
        val used = SiteLockPolicy.usedMillisForToday(site, now) + elapsedMillis
        var unlockUntil = site.unlockUntil
        val capped = if (site.dailyBudgetMinutes > 0) {
            val cap = SiteLockPolicy.budgetCapMillis(site.dailyBudgetMinutes)
            if (used >= cap) {
                unlockUntil = 0L
                cap
            } else {
                used
            }
        } else {
            used
        }
        val updated = site.copy(
            usedMillisToday = capped,
            budgetDayEpoch = day,
            unlockUntil = unlockUntil
        )
        applyLocal(updated)
        repository.updateBlockedSiteUsage(
            updated.host,
            updated.usedMillisToday,
            updated.budgetDayEpoch,
            updated.unlockUntil
        )
    }

    private suspend fun requirePassword(password: String): String? = withContext(Dispatchers.Default) {
        when (val result = passwordStore.verify(password)) {
            PasswordVerifyResult.Ok -> null
            PasswordVerifyResult.NoPassword -> "No lock password is set"
            is PasswordVerifyResult.Wrong -> wrongPasswordMessage(result)
            is PasswordVerifyResult.Locked -> LockPasswordStore.lockoutMessage(result.remainingMillis)
        }
    }

    private fun applyLocal(site: BlockedSite) {
        val without = _sites.value.filter { it.host != site.host }
        replaceSnapshot(listOf(site) + without)
    }

    private fun removeLocal(host: String) {
        replaceSnapshot(_sites.value.filter { it.host != host })
    }

    private fun replaceSnapshot(list: List<BlockedSite>) {
        _sites.value = list
        _blockedHosts.value = list.map { it.host }.toSet()
    }

    companion object {
        val TIMED_UNLOCK_MINUTES = intArrayOf(5, 15, 30, 60)

        fun wrongPasswordMessage(result: PasswordVerifyResult.Wrong): String {
            return if (result.lockoutMillis > 0) {
                LockPasswordStore.lockoutMessage(result.lockoutMillis)
            } else {
                val left = (5 - result.failedAttempts).coerceAtLeast(0)
                if (left == 0) "Incorrect password"
                else "Incorrect password · $left attempt${if (left == 1) "" else "s"} left"
            }
        }
    }
}
