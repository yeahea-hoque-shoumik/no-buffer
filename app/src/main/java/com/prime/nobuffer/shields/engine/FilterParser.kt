package com.prime.nobuffer.shields.engine

/**
 * Parses Adblock-Plus / uBlock-Origin filter syntax. Unsupported constructs (regex rules,
 * scriptlets, `:has-text`, `$csp`, `$redirect`, …) are skipped — never approximated — so a rule
 * never blocks more than the list author intended.
 */
object FilterParser {

    sealed interface Line {
        data class Network(val rule: NetworkRule) : Line
        data class Hide(val selector: String, val include: List<String>, val exclude: List<String>) : Line
        data class CosmeticException(val selector: String, val include: List<String>) : Line
    }

    private val UNSUPPORTED_SELECTOR_MARKERS = listOf(
        "+js(", ":has-text(", ":-abp-", ":xpath(", ":matches-css", ":contains(", ":upward(",
        ":remove(", ":style(", ":nth-ancestor(", ":watch-attr(", ":min-text-length(", ":others(", ":-js-"
    )

    private val SKIPPED_OPTIONS = setOf(
        "document", "doc", "popup", "elemhide", "ehide", "generichide", "ghide", "genericblock",
        "inline-script", "inline-font", "csp", "redirect", "redirect-rule", "removeparam", "queryprune",
        "replace", "badfilter", "denyallow", "header", "cname", "permissions", "rewrite", "empty",
        "mp4", "to", "from", "method", "webrtc"
    )

    fun parse(rawLine: String): Line? {
        val line = rawLine.trim()
        if (line.isEmpty() || line[0] == '!' || line.startsWith("[")) return null
        if ("##" in line || "#@#" in line) return parseCosmetic(line) // rejected cosmetic lines must not fall through as network rules
        if ("#?#" in line || "#$#" in line || "#%#" in line) return null
        return parseNetwork(line)?.let { Line.Network(it) }
    }

    private fun parseCosmetic(line: String): Line? {
        val hideIdx = line.indexOf("##")
        val excIdx = line.indexOf("#@#")
        val (idx, isException) = when {
            hideIdx == -1 && excIdx == -1 -> return null
            excIdx != -1 && (hideIdx == -1 || excIdx < hideIdx) -> excIdx to true
            else -> hideIdx to false
        }
        val selector = line.substring(idx + if (isException) 3 else 2).trim()
        if (selector.isEmpty() || selector[0] == '^' || selector.length > 400) return null
        if ('{' in selector || '}' in selector || UNSUPPORTED_SELECTOR_MARKERS.any { it in selector }) return null

        val include = ArrayList<String>()
        val exclude = ArrayList<String>()
        val domainPart = line.substring(0, idx)
        if (domainPart.isNotEmpty()) {
            for (d in domainPart.split(',')) {
                val domain = d.trim().lowercase()
                if (domain.isEmpty() || '*' in domain) return null
                if (domain.startsWith("~")) exclude += domain.substring(1) else include += domain
            }
        }
        return if (isException) Line.CosmeticException(selector, include) else Line.Hide(selector, include, exclude)
    }

    fun parseNetwork(rawLine: String): NetworkRule? {
        var line = rawLine
        val exception = line.startsWith("@@")
        if (exception) line = line.substring(2)
        if (line.startsWith("/") && line.endsWith("/") && line.length > 1) return null // regex rule

        var options = ""
        val dollar = line.lastIndexOf('$')
        if (dollar != -1) {
            options = line.substring(dollar + 1)
            line = line.substring(0, dollar)
        }

        var hostAnchor = false
        var startAnchor = false
        var endAnchor = false
        if (line.startsWith("||")) { hostAnchor = true; line = line.substring(2) }
        else if (line.startsWith("|")) { startAnchor = true; line = line.substring(1) }
        if (line.endsWith("|")) { endAnchor = true; line = line.dropLast(1) }
        if (!hostAnchor && !startAnchor) line = line.trimStart('*')
        if (!endAnchor) line = line.trimEnd('*')
        line = line.lowercase()
        if (line.isEmpty() || line.any { it.code > 127 || it.isWhitespace() }) return null

        var party = 0
        var include = 0
        var exclude = 0
        var important = false
        var includeDomains: List<String>? = null
        var excludeDomains: List<String>? = null

        if (options.isNotEmpty()) {
            for (opt in options.split(',')) {
                val negated = opt.startsWith("~")
                val body = (if (negated) opt.substring(1) else opt).lowercase()
                val name = body.substringBefore('=')
                when {
                    name == "third-party" || name == "3p" -> party = if (negated) 2 else 1
                    name == "first-party" || name == "1p" -> party = if (negated) 1 else 2
                    name == "important" -> important = true
                    name == "match-case" || name == "all" -> Unit
                    name == "domain" -> {
                        val inc = ArrayList<String>()
                        val exc = ArrayList<String>()
                        for (d in body.substringAfter('=').split('|')) {
                            if (d.isEmpty() || '*' in d) return null
                            if (d.startsWith("~")) exc += d.substring(1) else inc += d
                        }
                        includeDomains = inc.ifEmpty { null }
                        excludeDomains = exc.ifEmpty { null }
                    }
                    name in SKIPPED_OPTIONS -> return null
                    else -> {
                        val bit = RequestType.fromOptionName(name) ?: return null
                        if (negated) exclude = exclude or bit else include = include or bit
                    }
                }
            }
        }
        val typeMask = when {
            include != 0 -> include and exclude.inv()
            exclude != 0 -> RequestType.ALL and exclude.inv()
            else -> RequestType.ALL
        }
        if (typeMask == 0) return null
        return NetworkRule(line, hostAnchor, startAnchor, endAnchor, party, typeMask, includeDomains, excludeDomains, important, exception)
    }
}
