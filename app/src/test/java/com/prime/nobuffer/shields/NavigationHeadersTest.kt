package com.prime.nobuffer.shields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationHeadersTest {

    @Test
    fun neitherFlagYieldsEmptyMap() {
        assertTrue(NavigationHeaders.build(doNotTrack = false, globalPrivacyControl = false).isEmpty())
    }

    @Test
    fun doNotTrackOnly() {
        assertEquals(
            mapOf("DNT" to "1"),
            NavigationHeaders.build(doNotTrack = true, globalPrivacyControl = false)
        )
    }

    @Test
    fun globalPrivacyControlOnly() {
        assertEquals(
            mapOf("Sec-GPC" to "1", "Accept-Language" to "en-US"),
            NavigationHeaders.build(doNotTrack = false, globalPrivacyControl = true)
        )
    }

    @Test
    fun bothFlagsCombineIndependently() {
        assertEquals(
            mapOf("DNT" to "1", "Sec-GPC" to "1", "Accept-Language" to "en-US"),
            NavigationHeaders.build(doNotTrack = true, globalPrivacyControl = true)
        )
    }
}
