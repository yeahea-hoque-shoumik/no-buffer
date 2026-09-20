package com.prime.nobuffer.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedHostMatcherTest {

    @Test
    fun normalizesUrlsToHost() {
        assertEquals("youtube.com", BlockedHostMatcher.normalizeHost("https://www.youtube.com/watch?v=abc"))
        assertEquals("youtube.com", BlockedHostMatcher.normalizeHost("YOUTUBE.COM"))
        assertEquals("m.youtube.com", BlockedHostMatcher.normalizeHost("https://m.youtube.com"))
        assertEquals("youtube.com", BlockedHostMatcher.normalizeHost("youtube.com:443/watch"))
        assertNull(BlockedHostMatcher.normalizeHost("not a domain"))
        assertNull(BlockedHostMatcher.normalizeHost("localhost"))
    }

    @Test
    fun blockingApexBlocksSubdomains() {
        val blocked = setOf("youtube.com")
        assertTrue(BlockedHostMatcher.isBlocked("https://youtube.com", blocked))
        assertTrue(BlockedHostMatcher.isBlocked("https://www.youtube.com/watch?v=1", blocked))
        assertTrue(BlockedHostMatcher.isBlocked("https://m.youtube.com", blocked))
        assertTrue(BlockedHostMatcher.isBlocked("https://music.youtube.com", blocked))
        assertFalse(BlockedHostMatcher.isBlocked("https://youtu.be/abc", blocked))
        assertFalse(BlockedHostMatcher.isBlocked("https://google.com", blocked))
        assertFalse(BlockedHostMatcher.isBlocked("about:blank", blocked))
    }

    @Test
    fun moreSpecificBlockDoesNotBlockApex() {
        val blocked = setOf("m.youtube.com")
        assertTrue(BlockedHostMatcher.isBlocked("https://m.youtube.com/watch", blocked))
        assertFalse(BlockedHostMatcher.isBlocked("https://youtube.com", blocked))
        assertFalse(BlockedHostMatcher.isBlocked("https://www.youtube.com", blocked))
    }
}
