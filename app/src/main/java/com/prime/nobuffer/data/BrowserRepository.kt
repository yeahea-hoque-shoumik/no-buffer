package com.prime.nobuffer.data

import com.prime.nobuffer.data.entity.Bookmark
import com.prime.nobuffer.data.entity.CustomFilterRule
import com.prime.nobuffer.data.entity.HistoryEntry
import com.prime.nobuffer.data.entity.PermissionType
import com.prime.nobuffer.data.entity.SiteCosmeticRule
import com.prime.nobuffer.data.entity.SitePermissionGrant
import com.prime.nobuffer.data.entity.SiteShieldOverride
import com.prime.nobuffer.data.entity.TabEntity
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

    // --- Tabs ---

    suspend fun getSavedTabs(): List<TabEntity> = db.tabDao().getAll()

    suspend fun saveTabs(tabs: List<TabEntity>) {
        db.tabDao().clearAll()
        db.tabDao().insertAll(tabs)
    }

    // --- Shields (Phase 17/20) ---

    suspend fun upsertSiteShieldOverride(override: SiteShieldOverride) =
        db.siteShieldOverrideDao().upsert(override)

    suspend fun findSiteShieldOverride(host: String): SiteShieldOverride? =
        db.siteShieldOverrideDao().findByHost(host)

    suspend fun deleteSiteShieldOverride(host: String) = db.siteShieldOverrideDao().delete(host)

    fun observeSiteShieldOverrides(): Flow<List<SiteShieldOverride>> =
        db.siteShieldOverrideDao().observeAll()

    suspend fun addSiteCosmeticRule(host: String, selector: String) =
        db.siteCosmeticRuleDao().insert(SiteCosmeticRule(host = host, selector = selector))

    fun observeSiteCosmeticRules(): Flow<List<SiteCosmeticRule>> =
        db.siteCosmeticRuleDao().observeAll()

    suspend fun importCustomFilterRules(rules: List<CustomFilterRule>) {
        db.customFilterRuleDao().insertAll(rules)
    }

    suspend fun clearCustomFilterRules() = db.customFilterRuleDao().clearAll()

    suspend fun customFilterRuleCount(): Int = db.customFilterRuleDao().count()

    fun observeCustomFilterRules(): Flow<List<CustomFilterRule>> =
        db.customFilterRuleDao().observeAll()

    // --- Permission grants (Phase 21) ---

    suspend fun findActivePermissionGrant(host: String, type: PermissionType): SitePermissionGrant? =
        db.sitePermissionGrantDao().findActive(host, type, System.currentTimeMillis())

    suspend fun grantSitePermission(host: String, type: PermissionType, ttlMillis: Long) {
        val now = System.currentTimeMillis()
        db.sitePermissionGrantDao().upsert(
            SitePermissionGrant(host = host, permissionType = type, grantedAt = now, expiresAt = now + ttlMillis)
        )
    }

    suspend fun purgeExpiredPermissionGrants() =
        db.sitePermissionGrantDao().deleteExpired(System.currentTimeMillis())
}
