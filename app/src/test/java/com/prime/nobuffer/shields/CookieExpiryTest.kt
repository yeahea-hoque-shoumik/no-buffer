package com.prime.nobuffer.shields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieExpiryTest {

    @Test
    fun namesFromCookieHeader() {
        assertEquals(
            listOf("sid", "pref"),
            CookieExpiry.cookieNames("sid=abc; pref=1")
        )
    }

    @Test
    fun expirePayloadsCoverPathAndDomain() {
        val payloads = CookieExpiry.expirePayloads("sid=abc", "www.Example.com")
        assertEquals(3, payloads.size)
        assertTrue(payloads.all { it.startsWith("sid=") })
        assertTrue(payloads.any { it.contains("Path=/") && !it.contains("Domain=") })
        assertTrue(payloads.any { it.contains("Domain=example.com") })
        assertTrue(payloads.any { it.contains("Domain=.example.com") })
        assertTrue(payloads.all { it.contains("Max-Age=0") })
    }

    @Test
    fun urlsForHostIncludeApexAndWww() {
        val urls = CookieExpiry.urlsForHost("News.example.com")
        assertEquals(listOf("https://news.example.com/", "https://www.news.example.com/"), urls)
    }

    @Test
    fun emptyHostYieldsNothing() {
        assertTrue(CookieExpiry.expirePayloads("a=1", " ").isEmpty())
        assertTrue(CookieExpiry.urlsForHost("").isEmpty())
        assertTrue(CookieExpiry.cookieNames("").isEmpty())
        assertTrue(CookieExpiry.cookieNames("=").isEmpty())
    }
}
