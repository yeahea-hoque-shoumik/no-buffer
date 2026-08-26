package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("""
        SELECT * FROM history
        WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'
        ORDER BY visitedAt DESC
    """)
    suspend fun search(query: String): List<HistoryEntry>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<HistoryEntry>>

    @Query("""
        SELECT * FROM history
        WHERE url = :url
        ORDER BY visitedAt DESC
        LIMIT 1
    """)
    suspend fun findByUrl(url: String): HistoryEntry?
}
