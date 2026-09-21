package com.prime.nobuffer.omnibox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchEnginesTest {

    @Test
    fun builtinUrlsEncodeQuery() {
        val google = SearchEngines.BUILTIN.first { it.name == "Google" }
        val bing = SearchEngines.BUILTIN.first { it.name == "Bing" }
        val ddg = SearchEngines.BUILTIN.first { it.name == "DuckDuckGo" }

        assertEquals(
            "https://www.google.com/search?q=hello+world",
            SearchEngines.searchUrl(google, "hello world")
        )
        assertEquals(
            "https://www.bing.com/search?q=a%26b",
            SearchEngines.searchUrl(bing, "a&b")
        )
        assertEquals(
            "https://duckduckgo.com/?q=kotlin",
            SearchEngines.searchUrl(ddg, "kotlin")
        )
        assertNull(google.suggestUrl)
        assertNull(bing.suggestUrl)
        assertNull(ddg.suggestUrl)
        assertNull(SearchEngines.suggestEndpoint(google, "cats"))
    }

    @Test
    fun allMergesCustomJson() {
        val json = """
            [
              {"name":"Startpage","searchUrl":"https://www.startpage.com/sp/search?q=%s","suggestUrl":"https://www.startpage.com/osuggestions?q=%s"},
              {"name":"Bad","searchUrl":"https://example.com/search"}
            ]
        """.trimIndent()
        val all = SearchEngines.all(json)
        assertEquals(SearchEngines.BUILTIN.size + 1, all.size)
        val startpage = all.last()
        assertEquals("Startpage", startpage.name)
        assertEquals(
            "https://www.startpage.com/sp/search?q=hello+world",
            SearchEngines.searchUrl(startpage, "hello world")
        )
        assertEquals("Startpage", SearchEngines.selected("Startpage", json).name)
    }

    @Test
    fun invalidJsonIsIgnored() {
        assertEquals(SearchEngines.BUILTIN, SearchEngines.all(""))
        assertEquals(SearchEngines.BUILTIN, SearchEngines.all("{not an array}"))
        assertEquals(SearchEngines.BUILTIN, SearchEngines.all("null"))
        assertEquals(SearchEngines.BUILTIN, SearchEngines.all("[{broken"))
        assertEquals("DuckDuckGo", SearchEngines.selected("Unknown", "nope").name)
    }

    @Test
    fun encodeCustomRoundTrips() {
        val spec = SearchEngineSpec("Startpage", "https://www.startpage.com/sp/search?q=%s", null)
        val json = SearchEngines.encodeCustom(listOf(spec))
        val parsed = SearchEngines.parseCustom(json)
        assertEquals(1, parsed.size)
        assertEquals("Startpage", parsed[0].name)
        assertNull(parsed[0].suggestUrl)
    }
}
