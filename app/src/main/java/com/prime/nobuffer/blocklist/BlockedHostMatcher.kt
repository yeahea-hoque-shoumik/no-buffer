package com.prime.nobuffer.blocklist

/**
 * Host-level site lock. Blocking `youtube.com` also matches `m.youtube.com` / `www.youtube.com`.
 * Stored hosts are normalized (lowercase, no scheme/path/port, `www.` stripped).
 */
object BlockedHostMatcher {

    fun normalizeHost(raw: String): String? {
        var s = raw.trim().lowercase()
        if (s.isEmpty()) return null
        if (s.startsWith("https://")) s = s.removePrefix("https://")
        else if (s.startsWith("http://")) s = s.removePrefix("http://")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#')
        if (s.startsWith("[") && ']' in s) {
            s = s.substringAfter('[').substringBefore(']')
        } else {
            val colon = s.lastIndexOf(':')
            if (colon > 0 && s.substring(colon + 1).all { it.isDigit() }) {
                s = s.substring(0, colon)
            }
        }
        s = s.trim('.')
        if (s.startsWith("www.")) s = s.removePrefix("www.")
        if (s.isBlank()) return null
        if (s.any { it.isWhitespace() || it == '/' || it == '\\' }) return null
        if ('.' !in s) return null
        return s
    }

    fun hostFromUrl(url: String): String? = normalizeHost(url)

    fun isBlocked(urlOrHost: String, blockedHosts: Set<String>): Boolean {
        if (blockedHosts.isEmpty()) return false
        val host = hostFromUrl(urlOrHost) ?: return false
        var current = host
        while (true) {
            if (current in blockedHosts) return true
            val dot = current.indexOf('.')
            if (dot <= 0) return false
            current = current.substring(dot + 1)
        }
    }
}
