package com.prime.nobuffer.shields.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FilterEngineTest {

    private fun engine(vararg lines: String) = FilterEngine.Builder().addLines(lines.asSequence()).build()

    @Test
    fun hostAnchorMatchesSubdomainsButNotLookalikes() {
        val e = engine("||ads.example.com^")
        assertTrue(e.shouldBlock("https://ads.example.com/x.js", "site.com", RequestType.SCRIPT))
        assertTrue(e.shouldBlock("https://cdn.ads.example.com/x.js", "site.com", RequestType.SCRIPT))
        assertFalse(e.shouldBlock("https://notads.example.com/x.js", "site.com", RequestType.SCRIPT))
        assertFalse(e.shouldBlock("https://example.com/ads.example.com/", "site.com", RequestType.SCRIPT))
    }

    @Test
    fun wildcardSeparatorAndAnchors() {
        val e = engine("/banner/*/img^", "|https://x.com/exact|")
        assertTrue(e.shouldBlock("https://a.com/banner/300/img?x=1", null, RequestType.IMAGE))
        assertFalse(e.shouldBlock("https://a.com/banner/300/imgs", null, RequestType.IMAGE))
        assertTrue(e.shouldBlock("https://x.com/exact", null, RequestType.OTHER))
        assertFalse(e.shouldBlock("https://x.com/exact/more", null, RequestType.OTHER))
    }

    @Test
    fun thirdPartyOption() {
        val e = engine("||tracker.net^\$third-party")
        assertTrue(e.shouldBlock("https://tracker.net/p.gif", "news.com", RequestType.IMAGE))
        assertFalse(e.shouldBlock("https://tracker.net/p.gif", "tracker.net", RequestType.IMAGE))
        assertFalse(e.shouldBlock("https://tracker.net/p.gif", "www.tracker.net", RequestType.IMAGE))
    }

    @Test
    fun typeOptions() {
        val e = engine("||cdn.com/lib\$script", "||cdn.com/pic\$~image")
        assertTrue(e.shouldBlock("https://cdn.com/lib.js", null, RequestType.SCRIPT))
        assertFalse(e.shouldBlock("https://cdn.com/lib.png", null, RequestType.IMAGE))
        assertFalse(e.shouldBlock("https://cdn.com/pic.png", null, RequestType.IMAGE))
        assertTrue(e.shouldBlock("https://cdn.com/pic.js", null, RequestType.SCRIPT))
    }

    @Test
    fun domainOption() {
        val e = engine("||ads.com^\$domain=a.com|~sub.a.com")
        assertTrue(e.shouldBlock("https://ads.com/x", "a.com", RequestType.OTHER))
        assertTrue(e.shouldBlock("https://ads.com/x", "www.a.com", RequestType.OTHER))
        assertFalse(e.shouldBlock("https://ads.com/x", "sub.a.com", RequestType.OTHER))
        assertFalse(e.shouldBlock("https://ads.com/x", "b.com", RequestType.OTHER))
    }

    @Test
    fun exceptionsWinUnlessImportant() {
        val e = engine("||ads.com^", "@@||ads.com/ok^", "||bad.com^\$important", "@@||bad.com^")
        assertTrue(e.shouldBlock("https://ads.com/x", null, RequestType.OTHER))
        assertFalse(e.shouldBlock("https://ads.com/ok", null, RequestType.OTHER))
        assertTrue(e.shouldBlock("https://bad.com/x", null, RequestType.OTHER))
    }

    @Test
    fun unsupportedRulesAreSkipped() {
        assertNull(FilterParser.parseNetwork("/ads[0-9]+/"))
        assertNull(FilterParser.parseNetwork("||x.com^\$csp=script-src 'none'"))
        assertNull(FilterParser.parseNetwork("||x.com^\$redirect=noop.js"))
        assertNull(FilterParser.parse("example.com##+js(aopr, foo)"))
        assertNull(FilterParser.parse("example.com##div:has-text(Ad)"))
        assertNull(FilterParser.parse("! comment"))
    }

    @Test
    fun cosmeticRules() {
        val e = engine("##.ad", "site.com##.promo", "site.com#@#.ad", "~other.com##.sponsor", "##.keep")
        assertEquals(setOf(".ad", ".keep", ".sponsor"), e.cosmetic.selectorsFor("random.org").toSet())
        assertEquals(setOf(".promo", ".keep", ".sponsor"), e.cosmetic.selectorsFor("www.site.com").toSet())
        assertEquals(setOf(".ad", ".keep"), e.cosmetic.selectorsFor("other.com").toSet())
    }

    @Test
    fun requestTypeResolution() {
        assertEquals(RequestType.SCRIPT, RequestType.resolve("https://a.com/x.js?v=1", emptyMap(), false))
        assertEquals(RequestType.IMAGE, RequestType.resolve("https://a.com/x", mapOf("Accept" to "image/webp,*/*"), false))
        assertEquals(RequestType.XHR, RequestType.resolve("https://a.com/api", mapOf("X-Requested-With" to "XMLHttpRequest"), false))
        assertEquals(RequestType.SUBDOCUMENT, RequestType.resolve("https://a.com/", emptyMap(), true))
    }

    @Test
    fun domainUtils() {
        assertEquals("example.co.uk", DomainUtils.registrableDomain("a.b.example.co.uk"))
        assertEquals("example.com", DomainUtils.registrableDomain("www.example.com"))
        assertEquals("btv.com.bd", DomainUtils.registrableDomain("www.btv.com.bd"))
        assertEquals("ads.com", DomainUtils.hostOf("https://user@ADS.com:8080/p?q"))
    }

    /** Runs against the bundled lists: they must parse, block known ad hosts, and load fast enough for startup. */
    @Test
    fun bundledListsLoadAndBlock() {
        val dir = File("src/main/assets/blocklists")
        val start = System.nanoTime()
        val ads = File(dir, "easylist.txt").bufferedReader().use { FilterEngine.Builder().addLines(it.lineSequence()).build() }
        val trackers = File(dir, "easyprivacy.txt").bufferedReader().use { FilterEngine.Builder().addLines(it.lineSequence()).build() }
        val loadMs = (System.nanoTime() - start) / 1_000_000
        println("engine load: ${loadMs}ms ads=${ads.networkRuleCount} trackers=${trackers.networkRuleCount} generic=${ads.cosmetic.genericCount}")

        assertTrue(ads.networkRuleCount > 10_000)
        assertTrue(trackers.networkRuleCount > 5_000)
        assertTrue(ads.shouldBlock("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "news.com", RequestType.SCRIPT))
        assertTrue(trackers.shouldBlock("https://www.google-analytics.com/analytics.js", "news.com", RequestType.SCRIPT))
        assertFalse(ads.shouldBlock("https://en.wikipedia.org/wiki/Main_Page", "en.wikipedia.org", RequestType.SUBDOCUMENT))
        assertTrue("load too slow: ${loadMs}ms", loadMs < 8_000)

        val t0 = System.nanoTime()
        repeat(1_000) { ads.shouldBlock("https://cdn.example.com/assets/app.$it.js", "example.com", RequestType.SCRIPT) }
        val perReqUs = (System.nanoTime() - t0) / 1_000 / 1_000
        println("per-request: ${perReqUs}us")
        assertTrue("match too slow: ${perReqUs}us", perReqUs < 2_000)
    }
}
