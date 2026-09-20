package com.prime.nobuffer.omnibox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionParserTest {

    @Test
    fun parsesGoogleFirefoxJson() {
        val body = """["kotlin",["kotlin lang","kotlin coroutines","kotlinx"]]"""
        assertEquals(
            listOf("kotlin lang", "kotlin coroutines", "kotlinx"),
            SearchSuggestionParser.parse(body)
        )
    }

    @Test
    fun parsesBingOsjsonSameAsGoogle() {
        val body = """["query",["one","two"],"extra"]"""
        assertEquals(listOf("one", "two"), SearchSuggestionParser.parse(body))
    }

    @Test
    fun parsesDuckDuckGoJson() {
        val body = """[{"phrase":"kotlin"},{"phrase":"kotlin playground"}]"""
        assertEquals(
            listOf("kotlin", "kotlin playground"),
            SearchSuggestionParser.parse(body)
        )
    }

    @Test
    fun invalidOrEmptyBodyReturnsEmpty() {
        assertTrue(SearchSuggestionParser.parse("").isEmpty())
        assertTrue(SearchSuggestionParser.parse("not json").isEmpty())
        assertTrue(SearchSuggestionParser.parse("{}").isEmpty())
    }
}
