package com.prime.nobuffer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderModeTest {

    private val sampleJson =
        """{"title":"Hello","byline":"Ann","contentHtml":"<p>Hi</p>"}"""

    @Test
    fun parsesRawJson() {
        val article = ReaderMode.parseExtractResult(sampleJson)
        assertNotNull(article)
        assertEquals("Hello", article!!.title)
        assertEquals("Ann", article.byline)
        assertEquals("<p>Hi</p>", article.contentHtml)
    }

    @Test
    fun parsesWebViewQuotedJsString() {
        val quoted = "\"{\\\"title\\\":\\\"Hello\\\",\\\"byline\\\":\\\"Ann\\\",\\\"contentHtml\\\":\\\"<p>Hi</p>\\\"}\""
        val article = ReaderMode.parseExtractResult(quoted)
        assertNotNull(article)
        assertEquals("Hello", article!!.title)
        assertEquals("Ann", article.byline)
        assertEquals("<p>Hi</p>", article.contentHtml)
    }

    @Test
    fun emptyBylineBecomesNull() {
        val article = ReaderMode.parseExtractResult("""{"title":"T","byline":"","contentHtml":"<p>x</p>"}""")
        assertNotNull(article)
        assertEquals("T", article!!.title)
        assertNull(article.byline)
    }

    @Test
    fun nullEmptyAndInvalidReturnNull() {
        assertNull(ReaderMode.parseExtractResult(""))
        assertNull(ReaderMode.parseExtractResult("null"))
        assertNull(ReaderMode.parseExtractResult("\"null\""))
        assertNull(ReaderMode.parseExtractResult("undefined"))
        assertNull(ReaderMode.parseExtractResult("not json"))
    }
}
