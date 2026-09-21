package com.prime.nobuffer.browser

import android.webkit.CookieManager
import com.prime.nobuffer.blocklist.BlockedHostMatcher
import com.prime.nobuffer.shields.CookieExpiry

object SiteCookies {

    fun expireHost(hostOrUrl: String) {
        val host = hostFrom(hostOrUrl) ?: return
        val manager = CookieManager.getInstance()
        CookieExpiry.urlsForHost(host).forEach { url ->
            val header = manager.getCookie(url) ?: return@forEach
            CookieExpiry.expirePayloads(header, host).forEach { payload ->
                manager.setCookie(url, payload)
            }
        }
        manager.flush()
    }

    fun expireHosts(hosts: Collection<String>) {
        hosts.forEach { expireHost(it) }
    }

    fun hostFrom(hostOrUrl: String): String? = BlockedHostMatcher.normalizeHost(hostOrUrl)
}
