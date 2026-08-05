package com.prime.browser.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prime.browser.data.entity.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Update
    suspend fun update(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("""
        SELECT * FROM bookmarks
        WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'
        ORDER BY sortOrder ASC, createdAt DESC
    """)
    suspend fun search(query: String): List<Bookmark>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url AND isFolder = 0 LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE parentId IS :parentId ORDER BY sortOrder ASC, createdAt DESC")
    fun observeByParent(parentId: Long?): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE url = :url AND isFolder = 0 LIMIT 1")
    suspend fun findByUrl(url: String): Bookmark?
}
