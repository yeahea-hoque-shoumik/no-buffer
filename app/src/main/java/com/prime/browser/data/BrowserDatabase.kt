package com.prime.browser.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prime.browser.data.dao.BookmarkDao
import com.prime.browser.data.dao.HistoryDao
import com.prime.browser.data.entity.Bookmark
import com.prime.browser.data.entity.HistoryEntry

@Database(
    entities = [HistoryEntry::class, Bookmark::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
