package com.prime.nobuffer.shields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSanitizerTest {

    @Test
    fun unwrapsGoogleUrlRedirect() {
        val wrapped = "https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fpage"
        assertEquals("https://example.com/page", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun unwrapsGoogleCountryTldUrlRedirect() {
        val wrapped = "https://www.google.co.uk/url?url=https%3A%2F%2Fexample.com"
        assertEquals("https://example.com", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun unwrapsGoogleAclkAdurl() {
        val wrapped = "https://www.google.com/aclk?sa=L&adurl=https%3A%2F%2Fexample.com%2Fad"
        assertEquals("https://example.com/ad", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun unwrapsYoutubeRedirectQ() {
        val wrapped =
            "https://www.youtube.com/redirect?event=video_description&q=https%3A%2F%2Fexample.com"
        assertEquals("https://example.com", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun unwrapsInstagramLDot() {
        val wrapped = "https://l.instagram.com/?u=https%3A%2F%2Fexample.com%2Fpost&e=1"
        assertEquals("https://example.com/post", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun unwrapsFacebookLphpAndLm() {
        assertEquals(
            "https://example.com",
            UrlSanitizer.unwrapRedirector("https://www.facebook.com/l.php?u=https%3A%2F%2Fexample.com")
        )
        assertEquals(
            "https://example.com/x",
            UrlSanitizer.unwrapRedirector("https://lm.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fx")
        )
    }

    @Test
    fun unwrapsMozillaOutgoing() {
        assertEquals(
            "https://example.com",
            UrlSanitizer.unwrapRedirector("https://outgoing.prod.mozaws.net/?q=https%3A%2F%2Fexample.com")
        )
        assertEquals(
            "https://example.com/u",
            UrlSanitizer.unwrapRedirector("https://outgoing.mozilla.org/?u=https%3A%2F%2Fexample.com%2Fu")
        )
    }

    @Test
    fun unwrapsHrefLi() {
        assertEquals(
            "https://example.com/path",
            UrlSanitizer.unwrapRedirector("https://href.li/?https://example.com/path")
        )
    }

    @Test
    fun unwrapsDuckDuckGoUddg() {
        val wrapped = "https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage&rut=abc"
        assertEquals("https://example.com/page", UrlSanitizer.unwrapRedirector(wrapped))
    }

    @Test
    fun tCoIsLeftAsIs() {
        assertNull(UrlSanitizer.unwrapRedirector("https://t.co/abc123"))
    }

    @Test
    fun nonRedirectorReturnsNull() {
        assertNull(UrlSanitizer.unwrapRedirector("https://example.com/article"))
        assertNull(UrlSanitizer.unwrapRedirector("about:blank"))
    }

    @Test
    fun stripsLegacyAndNewTrackingParams() {
        val url = "https://example.com/a?id=1&utm_source=x&fbclid=abc&spm=1&vero_id=v" +
            "&mc_cid=c&oly_anon_id=o&oly_enc_id=e&icid=i&ndclid=n&twclid=t&ttclid=k" +
            "&ko_click_id=ko&li_fat_id=li&_hsenc=h&_hsmi=m&mkt_tok=tok&wickedid=w&keep=yes"
        val result = UrlSanitizer.stripTrackingParams(url)
        assertTrue(result.contains("id=1"))
        assertTrue(result.contains("keep=yes"))
        val stripped = listOf(
            "utm_source", "fbclid", "spm", "vero_id", "mc_cid", "oly_anon_id", "oly_enc_id",
            "icid", "ndclid", "twclid", "ttclid", "ko_click_id", "li_fat_id", "_hsenc",
            "_hsmi", "mkt_tok", "wickedid"
        )
        stripped.forEach { name ->
            assertFalse("$name should be stripped", result.contains(name))
        }
    }

    @Test
    fun stripLeavesUrlWithoutTrackingParamsUnchanged() {
        val url = "https://example.com/a?id=1&keep=yes"
        assertEquals(url, UrlSanitizer.stripTrackingParams(url))
    }
}
