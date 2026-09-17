package com.prime.nobuffer.shields

import com.prime.nobuffer.data.BrowserRepository
import com.prime.nobuffer.data.entity.CustomFilterRuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * In-memory mirror of per-site cosmetic hide rules (Phase 17: element picker + imported custom
 * filter list) and imported domain blocks, kept fresh from Room via background collectors so
 * [BrowserWebViewClient] callbacks can read them synchronously.
 */
class CosmeticRuleStore(private val repository: BrowserRepository, scope: CoroutineScope) {

    @Volatile private var siteRules: Map<String, List<String>> = emptyMap()
    @Volatile private var customCosmeticRules: Map<String, List<String>> = emptyMap()
    @Volatile private var customDomainBlocks: Set<String> = emptySet()

    init {
        repository.observeSiteCosmeticRules()
            .onEach { list -> siteRules = list.groupBy({ it.host.lowercase() }, { it.selector }) }
            .launchIn(scope)
        repository.observeCustomFilterRules()
            .onEach { list ->
                customCosmeticRules = list
                    .filter { it.type == CustomFilterRuleType.COSMETIC && !it.selector.isNullOrBlank() }
                    .groupBy({ it.domain.lowercase() }, { it.selector!! })
                customDomainBlocks = list
                    .filter { it.type == CustomFilterRuleType.DOMAIN }
                    .map { it.domain.lowercase() }
                    .toSet()
            }
            .launchIn(scope)
    }

    fun selectorsFor(host: String?): List<String> {
        val h = host?.lowercase() ?: return emptyList()
        return siteRules[h].orEmpty() + customCosmeticRules[h].orEmpty()
    }

    fun isDomainBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lower = host.lowercase()
        if (customDomainBlocks.contains(lower)) return true
        var idx = lower.indexOf('.')
        while (idx != -1) {
            if (customDomainBlocks.contains(lower.substring(idx + 1))) return true
            idx = lower.indexOf('.', idx + 1)
        }
        return false
    }

    suspend fun addSiteCosmeticRule(host: String, selector: String) =
        repository.addSiteCosmeticRule(host.lowercase(), selector)

    /** Parses [text] as a basic ABP-subset filter list and stores the resulting rules. Returns the rule count. */
    suspend fun importCustomFilterList(text: String): Int {
        val rules = CustomFilterListParser.parse(text)
        if (rules.isNotEmpty()) repository.importCustomFilterRules(rules)
        return rules.size
    }

    suspend fun clearCustomFilterList() = repository.clearCustomFilterRules()

    suspend fun customFilterRuleCount(): Int = repository.customFilterRuleCount()
}
