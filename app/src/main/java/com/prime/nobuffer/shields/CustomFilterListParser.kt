package com.prime.nobuffer.shields

import com.prime.nobuffer.data.entity.CustomFilterRule
import com.prime.nobuffer.data.entity.CustomFilterRuleType

/**
 * Parses a **basic subset** of Adblock Plus filter syntax (Phase 17 — explicitly not a full
 * ABP/uBlock engine):
 *  - Plain domain blocking: `||domain.com^`
 *  - Simple cosmetic hides: `domain.com##.selector`
 *
 * Everything else ($third-party/$domain= options, regex, scriptlets, `@@` exceptions, comments)
 * is ignored rather than mis-applied.
 */
object CustomFilterListParser {

    private val DOMAIN_RULE = Regex("^\\|\\|([a-zA-Z0-9.-]+)\\^$")
    private val COSMETIC_RULE = Regex("^([a-zA-Z0-9.,-]+)##(.+)$")

    fun parse(text: String): List<CustomFilterRule> {
        val rules = mutableListOf<CustomFilterRule>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("#") || line.startsWith("@@")) return@forEach

            DOMAIN_RULE.matchEntire(line)?.let { match ->
                rules += CustomFilterRule(type = CustomFilterRuleType.DOMAIN, domain = match.groupValues[1].lowercase())
                return@forEach
            }

            COSMETIC_RULE.matchEntire(line)?.let { match ->
                val selector = match.groupValues[2].trim()
                if (selector.isNotEmpty() && !selector.contains(':') /* skip :has()/:matches() etc — out of scope */) {
                    match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { domain ->
                        rules += CustomFilterRule(type = CustomFilterRuleType.COSMETIC, domain = domain.lowercase(), selector = selector)
                    }
                }
            }
        }
        return rules
    }
}
