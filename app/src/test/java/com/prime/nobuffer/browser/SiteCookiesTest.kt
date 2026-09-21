package com.prime.nobuffer.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteCookiesTest {

    @Test
    fun hostFromUrlStripsWwwAndPath() {
        assertEquals("example.com", SiteCookies.hostFrom("https://www.example.com/path?q=1"))
        assertEquals("example.com", SiteCookies.hostFrom("example.com"))
        assertEquals("news.example.com", SiteCookies.hostFrom("https://news.example.com"))
    }

    @Test
    fun hostFromRejectsGarbage() {
        assertNull(SiteCookies.hostFrom(""))
        assertNull(SiteCookies.hostFrom("localhost"))
        assertNull(SiteCookies.hostFrom("not a domain"))
    }
}
