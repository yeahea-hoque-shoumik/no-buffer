package com.prime.nobuffer.shields

/**
 * Builds `Set-Cookie` expiry payloads from a `Cookie` request header.
 * CookieManager has no per-host delete API, so we expire each name on `/`
 * and on the host / `.host` domains.
 */
object CookieExpiry {
    const val EPOCH_GMT = "Thu, 01 Jan 1970 00:00:00 GMT"

    fun cookieNames(cookieHeader: String): List<String> =
        cookieHeader.split(';')
            .map { it.trim().substringBefore('=').trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun expirePayloads(cookieHeader: String, host: String): List<String> {
        val cleanHost = host.trim().lowercase().removePrefix("www.")
        if (cleanHost.isEmpty()) return emptyList()
        return cookieNames(cookieHeader).flatMap { name ->
            listOf(
                "$name=; Expires=$EPOCH_GMT; Max-Age=0; Path=/",
                "$name=; Expires=$EPOCH_GMT; Max-Age=0; Path=/; Domain=$cleanHost",
                "$name=; Expires=$EPOCH_GMT; Max-Age=0; Path=/; Domain=.$cleanHost"
            )
        }
    }

    fun urlsForHost(host: String): List<String> {
        val cleanHost = host.trim().lowercase().removePrefix("www.")
        if (cleanHost.isEmpty()) return emptyList()
        return listOf(
            "https://$cleanHost/",
            "https://www.$cleanHost/"
        )
    }
}
