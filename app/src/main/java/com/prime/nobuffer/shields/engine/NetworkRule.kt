package com.prime.nobuffer.shields.engine

/**
 * One Adblock-Plus-syntax network rule. [pattern] is lower-case with anchors already stripped;
 * `*` is a wildcard and `^` a separator placeholder. Matching is done by hand (no Regex) so
 * ~100k rules stay cheap to hold in memory.
 */
class NetworkRule(
    val pattern: String,
    private val hostAnchor: Boolean,
    private val startAnchor: Boolean,
    private val endAnchor: Boolean,
    /** 0 = any, 1 = third-party only, 2 = first-party only. */
    private val party: Int,
    private val typeMask: Int,
    private val includeDomains: List<String>?,
    private val excludeDomains: List<String>?,
    val important: Boolean,
    val exception: Boolean
) {
    private val literalPrefix: String = pattern.takeWhile { it != '*' && it != '^' }

    fun matches(urlLower: String, requestHost: String?, firstPartyHost: String?, type: Int): Boolean {
        if ((typeMask and type) == 0) return false
        when (party) {
            1 -> if (!DomainUtils.isThirdParty(requestHost, firstPartyHost)) return false
            2 -> if (DomainUtils.isThirdParty(requestHost, firstPartyHost)) return false
        }
        if (includeDomains != null || excludeDomains != null) {
            val fp = firstPartyHost.orEmpty()
            if (includeDomains != null && includeDomains.none { DomainUtils.hostMatches(fp, it) }) return false
            if (excludeDomains != null && excludeDomains.any { DomainUtils.hostMatches(fp, it) }) return false
        }
        return matchesUrl(urlLower)
    }

    private fun matchesUrl(url: String): Boolean {
        if (startAnchor) return matchAt(url, 0, 0)
        if (hostAnchor) {
            val schemeEnd = url.indexOf("://")
            val hostStart = if (schemeEnd == -1) 0 else schemeEnd + 3
            var hostEnd = url.length
            for (i in hostStart until url.length) {
                val c = url[i]
                if (c == '/' || c == '?' || c == '#') { hostEnd = i; break }
            }
            if (matchAt(url, hostStart, 0)) return true
            var i = hostStart
            while (i < hostEnd) {
                if (url[i] == '.' && matchAt(url, i + 1, 0)) return true
                i++
            }
            return false
        }
        if (literalPrefix.isEmpty()) {
            for (i in 0..url.length) if (matchAt(url, i, 0)) return true
            return false
        }
        var from = url.indexOf(literalPrefix)
        while (from != -1) {
            if (matchAt(url, from, 0)) return true
            from = url.indexOf(literalPrefix, from + 1)
        }
        return false
    }

    private fun matchAt(url: String, urlStart: Int, patStart: Int): Boolean {
        var u = urlStart
        var p = patStart
        while (p < pattern.length) {
            when (val c = pattern[p]) {
                '*' -> {
                    while (p + 1 < pattern.length && pattern[p + 1] == '*') p++
                    if (p + 1 == pattern.length) return true
                    for (k in u..url.length) if (matchAt(url, k, p + 1)) return true
                    return false
                }
                '^' -> {
                    if (u == url.length) return p + 1 == pattern.length
                    if (!isSeparator(url[u])) return false
                    u++; p++
                }
                else -> {
                    if (u >= url.length || url[u] != c) return false
                    u++; p++
                }
            }
        }
        return !endAnchor || u == url.length
    }

    private fun isSeparator(c: Char): Boolean =
        !(c.isLetterOrDigit() || c == '_' || c == '-' || c == '.' || c == '%')

    /** Longest alphanumeric run in the pattern that is delimited on both sides, or null if none qualifies. */
    fun indexToken(): String? {
        var best: String? = null
        var i = 0
        while (i < pattern.length) {
            if (!isTokenChar(pattern[i])) { i++; continue }
            var j = i
            while (j < pattern.length && isTokenChar(pattern[j])) j++
            val leftOk = if (i == 0) (hostAnchor || startAnchor) else pattern[i - 1] != '*'
            val rightOk = if (j == pattern.length) endAnchor else pattern[j] != '*'
            val token = pattern.substring(i, j)
            if (leftOk && rightOk && token.length >= 3 && token !in COMMON_TOKENS && (best == null || token.length > best.length)) {
                best = token
            }
            i = j
        }
        return best
    }

    companion object {
        val COMMON_TOKENS = setOf("http", "https", "www", "com", "net", "org")
        fun isTokenChar(c: Char) = (c in 'a'..'z') || (c in '0'..'9')
    }
}
