package com.prime.nobuffer.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentlyClosedTest {

    private fun tab(
        url: String,
        title: String = url,
        incognito: Boolean = false,
        at: Long = 1L
    ) = RecentlyClosedTab(url = url, title = title, isIncognito = incognito, closedAt = at)

    @Test
    fun capsAtTwentyDroppingOldest() {
        val store = RecentlyClosedStore()
        for (i in 1..21) {
            store.push(tab("https://example.com/$i", at = i.toLong()))
        }
        assertEquals(20, store.items.value.size)
        assertEquals("https://example.com/21", store.items.value.first().url)
        assertEquals("https://example.com/2", store.items.value.last().url)
    }

    @Test
    fun skipsAboutBlankAndEmptyUrl() {
        val store = RecentlyClosedStore()
        store.push(tab("about:blank"))
        store.push(tab("ABOUT:BLANK"))
        store.push(tab(""))
        store.push(tab("   "))
        assertTrue(store.items.value.isEmpty())
    }

    @Test
    fun skipsIncognito() {
        val store = RecentlyClosedStore()
        store.push(tab("https://secret.example", incognito = true))
        assertTrue(store.items.value.isEmpty())
    }

    @Test
    fun popReturnsNewestFirst() {
        val store = RecentlyClosedStore()
        store.push(tab("https://a.example", at = 1))
        store.push(tab("https://b.example", at = 2))
        assertEquals("https://b.example", store.pop()?.url)
        assertEquals("https://a.example", store.pop()?.url)
        assertNull(store.pop())
        assertTrue(store.items.value.isEmpty())
    }

    @Test
    fun jsonRoundTripDropsIneligible() {
        val json = RecentlyClosedStore.toJson(
            listOf(
                tab("https://ok.example", "Ok", at = 9),
                tab("about:blank"),
                tab("https://secret.example", incognito = true)
            )
        )
        val restored = RecentlyClosedStore.fromJson(json)
        assertEquals(1, restored.size)
        assertEquals("https://ok.example", restored[0].url)
        assertEquals("Ok", restored[0].title)
        assertEquals(9L, restored[0].closedAt)
        assertEquals(restored, RecentlyClosedStore.fromJson(RecentlyClosedStore.toJson(restored)))
    }

    @Test
    fun invalidJsonYieldsEmpty() {
        assertTrue(RecentlyClosedStore.fromJson("").isEmpty())
        assertTrue(RecentlyClosedStore.fromJson("not json").isEmpty())
        assertTrue(RecentlyClosedStore.fromJson("{}").isEmpty())
    }
}
