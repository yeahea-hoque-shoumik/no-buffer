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
    scope: CoroutineScope
) {
    val sites: StateFlow<List<BlockedSite>> = repository.observeBlockedSites()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _blockedHosts = MutableStateFlow<Set<String>>(emptySet())
    val blockedHosts: StateFlow<Set<String>> = _blockedHosts.asStateFlow()

    val hasPassword: StateFlow<Boolean> = passwordStore.hasPassword
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        runBlocking(Dispatchers.IO) {
            _blockedHosts.value = repository.getBlockedHosts().toSet()
        }
        scope.launch {
            repository.observeBlockedSites().collect { list ->
                _blockedHosts.value = list.map { it.host }.toSet()
            }
        }
    }

    fun isUrlBlocked(url: String): Boolean =
        BlockedHostMatcher.isBlocked(url, blockedHosts.value)

    suspend fun addSite(raw: String): String? {
        if (!passwordStore.hasPasswordNow()) {
            return "Set a lock password before blocking sites"
        }
        val host = BlockedHostMatcher.normalizeHost(raw)
            ?: return "Enter a domain like youtube.com"
        repository.upsertBlockedSite(BlockedSite(host = host))
        _blockedHosts.value = _blockedHosts.value + host
        return null
    }

    suspend fun unlockSite(hostOrUrl: String, password: String): String? {
        val host = BlockedHostMatcher.normalizeHost(hostOrUrl)
            ?: return "Invalid site"
        return withContext(Dispatchers.Default) {
            when (val result = passwordStore.verify(password)) {
                PasswordVerifyResult.Ok -> {
                    // Remove the stored rule that actually matched, not just the typed host.
                    val matched = blockedHosts.value.firstOrNull { blocked ->
                        BlockedHostMatcher.isBlocked(host, setOf(blocked))
                    } ?: host
                    _blockedHosts.value = _blockedHosts.value - matched
                    repository.deleteBlockedSite(matched)
                    null
                }
                PasswordVerifyResult.NoPassword -> "No lock password is set"
                is PasswordVerifyResult.Wrong -> wrongPasswordMessage(result)
                is PasswordVerifyResult.Locked -> LockPasswordStore.lockoutMessage(result.remainingMillis)
            }
        }
    }

    suspend fun unlockAll(password: String): String? = withContext(Dispatchers.Default) {
        when (val result = passwordStore.verify(password)) {
            PasswordVerifyResult.Ok -> {
                _blockedHosts.value = emptySet()
                repository.clearBlockedSites()
                null
            }
            PasswordVerifyResult.NoPassword -> "No lock password is set"
            is PasswordVerifyResult.Wrong -> wrongPasswordMessage(result)
            is PasswordVerifyResult.Locked -> LockPasswordStore.lockoutMessage(result.remainingMillis)
        }
    }

    companion object {
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
