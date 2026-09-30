package com.prime.nobuffer.blocklist

import java.io.InputStream

/**
 * Forced, non-configurable gambling-site block. There is deliberately no setting, override or unlock
 * path: [SiteBlocker.isUrlBlocked] consults it before any user-managed lock.
 *
 * ~280k domains are held as sorted 64-bit FNV-1a hashes (2 MB) instead of strings (~25 MB); a
 * collision needs a 1-in-10^13 accident. A small brand/keyword pass catches new mirror domains
 * that no list has caught up with yet.
 */
class GamblingBlocklist(private val openList: () -> InputStream) {

    private val hashes: LongArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { load() }

    /** Forces the list into memory; call off the main thread at startup. */
    fun warmUp() {
        hashes
    }

    fun isBlocked(urlOrHost: String): Boolean {
        val host = BlockedHostMatcher.hostFromUrl(urlOrHost) ?: return false
        if (KEYWORDS.any { it in host }) return true
        val table = hashes
        var current = host
        while (true) {
            if (table.binarySearch(hash(current)) >= 0) return true
            val dot = current.indexOf('.')
            if (dot <= 0 || current.indexOf('.', dot + 1) == -1) return false // never test a bare TLD
            current = current.substring(dot + 1)
        }
    }

    private fun load(): LongArray {
        val out = LongArrayBuilder()
        runCatching {
            openList().bufferedReader().useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trim().lowercase()
                    if (line.isNotEmpty() && line[0] != '#') out.add(hash(line))
                }
            }
        }
        return out.toSortedArray()
    }

    private class LongArrayBuilder {
        private var data = LongArray(1 shl 16)
        private var size = 0
        fun add(v: Long) {
            if (size == data.size) data = data.copyOf(size * 2)
            data[size++] = v
        }
        fun toSortedArray(): LongArray = data.copyOf(size).also { it.sort() }
    }

    companion object {
        private val KEYWORDS = listOf(
            "casino", "sportsbook", "1xbet", "bet365", "melbet", "mostbet",
            "parimatch", "betwinner", "linebet", "22bet", "betway", "pokerstars"
        )

        fun hash(s: String): Long {
            var h = -0x340d631b7bdddcdbL // FNV-1a 64 offset basis
            for (c in s) {
                h = (h xor c.code.toLong()) * 0x100000001b3L
            }
            return h
        }
    }
}
