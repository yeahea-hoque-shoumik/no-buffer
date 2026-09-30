package com.prime.nobuffer.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GamblingBlocklistTest {

    private fun list(vararg hosts: String) = GamblingBlocklist { hosts.joinToString("\n").byteInputStream() }

    @Test
    fun matchesListedDomainAndSubdomains() {
        val l = list("# c", "badbets.example")
        assertTrue(l.isBlocked("https://badbets.example/"))
        assertTrue(l.isBlocked("https://www.badbets.example/x?y=1"))
        assertTrue(l.isBlocked("https://m.live.badbets.example:8443/"))
        assertFalse(l.isBlocked("https://notbadbets.example/"))
    }

    @Test
    fun keywordsCatchUnlistedMirrors() {
        val l = list()
        assertTrue(l.isBlocked("https://1xbet-mirror-77.xyz/"))
        assertTrue(l.isBlocked("https://best-casino-now.top/"))
        assertFalse(l.isBlocked("https://en.wikipedia.org/wiki/Casino"))
    }

    @Test
    fun hashIsStable() {
        assertEquals(GamblingBlocklist.hash("a.com"), GamblingBlocklist.hash("a.com"))
    }

    @Test
    fun bundledListBlocksAndAllowsNormalSites() {
        val file = File("src/main/assets/blocklists/gambling_hosts.txt")
        val l = GamblingBlocklist { file.inputStream() }
        val t = System.nanoTime()
        l.warmUp()
        println("gambling load: ${(System.nanoTime() - t) / 1_000_000}ms")
        val sample = file.useLines { lines -> lines.filter { !it.startsWith("#") }.drop(1000).first() }
        assertTrue(l.isBlocked("https://$sample/"))
        assertTrue(l.isBlocked("https://www.bet365.com/"))
        for (ok in listOf("google.com", "wikipedia.org", "github.com", "youtube.com", "bbc.co.uk", "prothomalo.com")) {
            assertFalse("$ok wrongly blocked", l.isBlocked("https://$ok/"))
        }
    }
}
