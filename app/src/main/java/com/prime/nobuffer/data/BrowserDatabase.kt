package com.prime.nobuffer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prime.nobuffer.data.dao.BookmarkDao
import com.prime.nobuffer.data.dao.HistoryDao
import com.prime.nobuffer.data.dao.TabDao
import com.prime.nobuffer.data.entity.Bookmark
import com.prime.nobuffer.data.entity.HistoryEntry
import com.prime.nobuffer.data.entity.TabEntity

@Database(
    entities = [HistoryEntry::class, Bookmark::class, TabEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tabDao(): TabDao
}
