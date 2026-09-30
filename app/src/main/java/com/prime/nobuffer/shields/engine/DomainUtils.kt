package com.prime.nobuffer.shields.engine

object DomainUtils {

    // Small eTLD+1 heuristic — avoids shipping the full public-suffix list.
    private val MULTI_PART_SUFFIXES = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk", "com.au", "net.au", "org.au", "co.jp", "co.in",
        "com.br", "com.cn", "co.nz", "co.za", "com.mx", "com.tr", "com.sg", "com.hk", "com.tw",
        "co.kr", "com.ar", "co.id", "com.my", "com.pk", "com.ng",
        "com.bd", "net.bd", "org.bd", "gov.bd", "edu.bd"
    )

    fun registrableDomain(host: String): String {
        val parts = host.split('.')
        if (parts.size <= 2 || host.all { it.isDigit() || it == '.' } || ':' in host) return host
        val lastTwo = parts.takeLast(2).joinToString(".")
        return if (lastTwo in MULTI_PART_SUFFIXES) parts.takeLast(3).joinToString(".") else lastTwo
    }

    fun isThirdParty(requestHost: String?, firstPartyHost: String?): Boolean {
        if (requestHost.isNullOrEmpty() || firstPartyHost.isNullOrEmpty()) return false
        return registrableDomain(requestHost) != registrableDomain(firstPartyHost)
    }

    fun hostMatches(host: String, domain: String): Boolean =
        host == domain || (host.length > domain.length && host.endsWith(domain) && host[host.length - domain.length - 1] == '.')

    /** Host of an absolute URL without allocating a Uri — lower-cased, no userinfo/port. */
    fun hostOf(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) return null
        val start = schemeEnd + 3
        var end = url.length
        for (i in start until url.length) {
            val c = url[i]
            if (c == '/' || c == '?' || c == '#') { end = i; break }
        }
        var authority = url.substring(start, end)
        authority.lastIndexOf('@').takeIf { it != -1 }?.let { authority = authority.substring(it + 1) }
        if (!authority.startsWith("[")) authority.indexOf(':').takeIf { it != -1 }?.let { authority = authority.substring(0, it) }
        return authority.lowercase().ifEmpty { null }
    }
}
