package com.prime.browser.data

import com.prime.browser.data.entity.Bookmark
import com.prime.browser.data.entity.HistoryEntry
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val db: BrowserDatabase) {

    // --- History ---

    suspend fun insertHistory(entry: HistoryEntry) = db.historyDao().insert(entry)

    suspend fun deleteHistory(id: Long) = db.historyDao().delete(id)

    suspend fun clearAllHistory() = db.historyDao().clearAll()

    suspend fun searchHistory(query: String): List<HistoryEntry> =
        db.historyDao().search(query)

    fun observeHistory(): Flow<List<HistoryEntry>> = db.historyDao().observeAll()

    suspend fun findHistoryByUrl(url: String): HistoryEntry? =
        db.historyDao().findByUrl(url)

    // --- Bookmarks ---

    suspend fun insertBookmark(bookmark: Bookmark): Long =
        db.bookmarkDao().insert(bookmark)

    suspend fun updateBookmark(bookmark: Bookmark) = db.bookmarkDao().update(bookmark)

    suspend fun deleteBookmark(id: Long) = db.bookmarkDao().delete(id)

    suspend fun searchBookmarks(query: String): List<Bookmark> =
        db.bookmarkDao().search(query)

    suspend fun isBookmarked(url: String): Boolean = db.bookmarkDao().isBookmarked(url)

    fun observeBookmarks(): Flow<List<Bookmark>> = db.bookmarkDao().observeAll()

    fun observeBookmarksByParent(parentId: Long?): Flow<List<Bookmark>> =
        db.bookmarkDao().observeByParent(parentId)

    suspend fun findBookmarkByUrl(url: String): Bookmark? =
        db.bookmarkDao().findByUrl(url)
}
