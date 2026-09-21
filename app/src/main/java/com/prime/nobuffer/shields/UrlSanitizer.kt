package com.prime.nobuffer.shields

import java.net.URI
import java.net.URLDecoder

/** Phase 19 — list-based navigation hardening: tracking-param stripping, redirector unwrapping, de-AMP. */
object UrlSanitizer {

    private val TRACKING_PARAM_PREFIXES = listOf("utm_")
    private val TRACKING_PARAMS = setOf(
        "fbclid", "gclid", "msclkid", "mc_eid", "igshid", "ref_src", "yclid", "dclid", "_ga",
        "spm", "vero_id", "mc_cid", "oly_anon_id", "oly_enc_id", "icid", "ndclid",
        "twclid", "ttclid", "ko_click_id", "li_fat_id", "_hsenc", "_hsmi", "mkt_tok", "wickedid"
    )

    private val AMP_HOSTS = listOf("cdn.ampproject.org", "amp.cloudflare.com")

    /**
     * Strips known tracking query params. Returns [url] unchanged if nothing matched.
     *
     * Only meaningful for hierarchical `http(s)://` URLs — opaque URIs like `about:blank`
     * are skipped outright.
     */
    fun stripTrackingParams(url: String): String {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return url
        val hashIdx = url.indexOf('#')
        val withoutFrag = if (hashIdx >= 0) url.substring(0, hashIdx) else url
        val frag = if (hashIdx >= 0) url.substring(hashIdx) else ""
        val qIdx = withoutFrag.indexOf('?')
        if (qIdx < 0) return url
        val base = withoutFrag.substring(0, qIdx)
        val query = withoutFrag.substring(qIdx + 1)
        if (query.isEmpty()) return url
        val originalParts = query.split('&')
        val kept = originalParts.filter { part ->
            if (part.isEmpty()) return@filter false
            val name = decode(part.substringBefore('=')).lowercase()
            name.isNotEmpty() &&
                name !in TRACKING_PARAMS &&
                TRACKING_PARAM_PREFIXES.none { name.startsWith(it) }
        }
        if (kept.size == originalParts.size) return url
        return if (kept.isEmpty()) base + frag else base + "?" + kept.joinToString("&") + frag
    }

    /** Unwraps a known redirector wrapper to its real target, or null if not a match. */
    fun unwrapRedirector(url: String): String? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return null
            val path = uri.path.orEmpty()
            // t.co cannot be unwrapped without a network hop — leave as-is
            val target = when {
                isGoogleHost(host) && (path == "/url" || path == "/aclk") ->
                    queryParam(uri, "q") ?: queryParam(uri, "url") ?: queryParam(uri, "adurl")
                isYoutubeHost(host) && path == "/redirect" ->
                    queryParam(uri, "q")
                host == "l.instagram.com" || host == "www.l.instagram.com" ->
                    queryParam(uri, "u")
                host.endsWith("facebook.com") && path == "/l.php" ->
                    queryParam(uri, "u")
                host == "outgoing.prod.mozaws.net" || host == "outgoing.mozilla.org" ->
                    queryParam(uri, "q") ?: queryParam(uri, "u")
                host == "href.li" || host == "www.href.li" -> {
                    val raw = uri.rawQuery ?: return@runCatching null
                    return@runCatching decode(raw)
                }
                isDuckDuckGoHost(host) && (path == "/l" || path == "/l/") ->
                    queryParam(uri, "uddg")
                else -> null
            } ?: return null
            target
        }.getOrNull()
    }

    fun isAmpUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase().orEmpty()
            if (AMP_HOSTS.any { host == it || host.endsWith(".$it") }) return true
            uri.path?.contains("/amp/") == true
        }.getOrDefault(false)
    }

    private fun isGoogleHost(host: String): Boolean {
        val h = host.removePrefix("www.")
        return h.startsWith("google.")
    }

    private fun isYoutubeHost(host: String): Boolean =
        host == "youtube.com" || host.endsWith(".youtube.com")

    private fun isDuckDuckGoHost(host: String): Boolean {
        val h = host.removePrefix("www.")
        return h == "duckduckgo.com" || h.endsWith(".duckduckgo.com")
    }

    private fun queryParam(uri: URI, name: String): String? {
        val raw = uri.rawQuery ?: return null
        raw.split('&').forEach { part ->
            val eq = part.indexOf('=')
            val key = if (eq < 0) part else part.substring(0, eq)
            if (key.equals(name, ignoreCase = true)) {
                val value = if (eq < 0) "" else part.substring(eq + 1)
                return decode(value)
            }
        }
        return null
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
