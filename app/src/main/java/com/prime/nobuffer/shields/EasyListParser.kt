package com.prime.nobuffer.shields

/**
 * Parses a **basic subset** of EasyList / Adblock Plus host rules into hostnames:
 *  - `||domain.com^`
 *  - plain host lines (`ads.example.com`)
 *
 * `$third-party` options, regex, scriptlets, cosmetic (`##`) rules, and `@@` exceptions
 * are ignored — same policy as [CustomFilterListParser].
 */
object EasyListParser {

    private val DOMAIN_RULE = Regex("^\\|\\|([a-zA-Z0-9.-]+)\\^$")

    fun parseHosts(text: String): List<String> {
        val hosts = LinkedHashSet<String>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() ||
                line.startsWith("!") ||
                line.startsWith("#") ||
                line.startsWith("@@")
            ) {
                return@forEach
            }
            // Options, regex, cosmetic, scriptlets are out of scope.
            if ('$' in line || line.startsWith("/") || "##" in line || "#@#" in line) {
                return@forEach
            }

            DOMAIN_RULE.matchEntire(line)?.let { match ->
                hosts += match.groupValues[1].lowercase()
                return@forEach
            }

            if (isPlainHost(line)) {
                hosts += line.lowercase()
            }
        }
        return hosts.toList()
    }

    private fun isPlainHost(line: String): Boolean {
        if (line.length < 3 || line.length > 253) return false
        if ('.' !in line) return false
        if (line.startsWith(".") || line.endsWith(".") || line.startsWith("-")) return false
        if (line.any { !it.isLetterOrDigit() && it != '.' && it != '-' }) return false
        return true
    }
}
