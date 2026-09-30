package com.prime.nobuffer.shields.engine

/** Immutable, thread-safe filter-list matcher. Build with [Builder], swap the whole instance to refresh. */
class FilterEngine private constructor(
    private val blocking: RuleIndex,
    private val exceptions: RuleIndex,
    val cosmetic: CosmeticIndex,
    val networkRuleCount: Int
) {

    fun shouldBlock(url: String, firstPartyHost: String?, type: Int): Boolean {
        val lower = url.lowercase()
        val host = DomainUtils.hostOf(lower)
        val fp = firstPartyHost?.lowercase()
        val hit = blocking.find(lower, host, fp, type) ?: return false
        if (hit.important) return true
        return exceptions.find(lower, host, fp, type) == null
    }

    private class RuleIndex(
        private val byToken: Map<String, Array<NetworkRule>>,
        private val untokenized: Array<NetworkRule>
    ) {
        fun find(url: String, host: String?, fp: String?, type: Int): NetworkRule? {
            var i = 0
            while (i < url.length) {
                if (!NetworkRule.isTokenChar(url[i])) { i++; continue }
                var j = i
                while (j < url.length && NetworkRule.isTokenChar(url[j])) j++
                if (j - i >= 3) {
                    byToken[url.substring(i, j)]?.let { candidates ->
                        for (rule in candidates) if (rule.matches(url, host, fp, type)) return rule
                    }
                }
                i = j
            }
            for (rule in untokenized) if (rule.matches(url, host, fp, type)) return rule
            return null
        }
    }

    class Builder {
        private val blockTokens = HashMap<String, MutableList<NetworkRule>>()
        private val blockRest = ArrayList<NetworkRule>()
        private val excTokens = HashMap<String, MutableList<NetworkRule>>()
        private val excRest = ArrayList<NetworkRule>()
        private val cosmetic = CosmeticIndex.Builder()
        private var count = 0

        fun addLine(line: String): Builder {
            when (val parsed = FilterParser.parse(line)) {
                null -> Unit
                is FilterParser.Line.Network -> addRule(parsed.rule)
                is FilterParser.Line.Hide -> cosmetic.addHide(parsed.selector, parsed.include, parsed.exclude)
                is FilterParser.Line.CosmeticException -> cosmetic.addException(parsed.selector, parsed.include)
            }
            return this
        }

        fun addLines(lines: Sequence<String>): Builder {
            lines.forEach { addLine(it) }
            return this
        }

        private fun addRule(rule: NetworkRule) {
            val tokens = if (rule.exception) excTokens else blockTokens
            val rest = if (rule.exception) excRest else blockRest
            val token = rule.indexToken()
            if (token != null) tokens.getOrPut(token) { ArrayList(2) } += rule else rest += rule
            count++
        }

        fun build(): FilterEngine = FilterEngine(
            RuleIndex(blockTokens.mapValues { it.value.toTypedArray() }, blockRest.toTypedArray()),
            RuleIndex(excTokens.mapValues { it.value.toTypedArray() }, excRest.toTypedArray()),
            cosmetic.build(),
            count
        )
    }
}
