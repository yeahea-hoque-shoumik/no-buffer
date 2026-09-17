package com.prime.nobuffer.shields

import android.net.Uri
import java.net.URLDecoder

/** Phase 19 — list-based navigation hardening: tracking-param stripping, redirector unwrapping, de-AMP. */
object UrlSanitizer {

    private val TRACKING_PARAM_PREFIXES = listOf("utm_")
    private val TRACKING_PARAMS = setOf(
        "fbclid", "gclid", "msclkid", "mc_eid", "igshid", "ref_src", "yclid", "dclid", "_ga"
    )

    private val AMP_HOSTS = listOf("cdn.ampproject.org", "amp.cloudflare.com")

    /**
     * Strips known tracking query params. Returns [url] unchanged if nothing matched.
     *
     * Only meaningful for hierarchical `http(s)://` URLs — opaque URIs like `about:blank` throw
     * `UnsupportedOperationException` from `Uri.getQueryParameterNames()`, so those are skipped
     * outright rather than relying on a catch to paper over it.
     */
    fun stripTrackingParams(url: String): String {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return url
        return runCatching {
            val uri = Uri.parse(url)
            val names = uri.queryParameterNames
            if (names.isEmpty()) return url
            val toStrip = names.filter { name ->
                val lower = name.lowercase()
                TRACKING_PARAMS.contains(lower) || TRACKING_PARAM_PREFIXES.any { lower.startsWith(it) }
            }
            if (toStrip.isEmpty()) return url

            val builder = uri.buildUpon().clearQuery()
            names.forEach { name ->
                if (name !in toStrip) {
                    uri.getQueryParameters(name).forEach { value -> builder.appendQueryParameter(name, value) }
                }
            }
            builder.build().toString()
        }.getOrDefault(url)
    }

    /** Unwraps a known redirector wrapper (Google `/url?q=`, Facebook `l.php?u=`) to its real target, or null if not a match. */
    fun unwrapRedirector(url: String): String? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        return runCatching {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return null
            val target = when {
                (host == "www.google.com" || host == "google.com" || host.startsWith("www.google.")) &&
                    uri.path == "/url" -> uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
                host.endsWith("facebook.com") && uri.path == "/l.php" -> uri.getQueryParameter("u")
                else -> null
            } ?: return null
            URLDecoder.decode(target, "UTF-8")
        }.getOrNull()
    }

    fun isAmpUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        return runCatching {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            if (AMP_HOSTS.any { host == it || host.endsWith(".$it") }) return true
            uri.path?.contains("/amp/") == true
        }.getOrDefault(false)
    }
}
