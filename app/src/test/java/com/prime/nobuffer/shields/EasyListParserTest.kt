package com.prime.nobuffer.shields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EasyListParserTest {

    @Test
    fun parsesDomainRuleAndPlainHost() {
        val hosts = EasyListParser.parseHosts(
            """
            ||ads.example.com^
            tracker.example.org
            """.trimIndent()
        )
        assertEquals(listOf("ads.example.com", "tracker.example.org"), hosts)
    }

    @Test
    fun ignoresThirdPartyOptionsRegexScriptletsAndExceptions() {
        val hosts = EasyListParser.parseHosts(
            """
            ! comment
            # also a comment
            ||ads.example.com^${'$'}third-party
            @@||exception.example.com^
            /banner\.js/
            example.com##.ad
            example.com#@#.keep
            ||keep.example.net^
            ||script.example.com^${'$'}script
            ##+js(abort-on-property-read, ads)
            """.trimIndent()
        )
        assertEquals(listOf("keep.example.net"), hosts)
    }

    @Test
    fun skipsEmptyAndInvalidLines() {
        val hosts = EasyListParser.parseHosts(
            """

            localhost
            not a host
            http://evil.example.com
            ||incomplete.example.com
            ||ok.example.com^
            """.trimIndent()
        )
        assertEquals(listOf("ok.example.com"), hosts)
        assertFalse(hosts.contains("localhost"))
        assertTrue(hosts.none { it.contains(" ") })
    }

    @Test
    fun lowercasesAndDedupes() {
        val hosts = EasyListParser.parseHosts(
            """
            ||Ads.Example.COM^
            ads.example.com
            """.trimIndent()
        )
        assertEquals(listOf("ads.example.com"), hosts)
    }
}
