package com.prime.nobuffer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.prime.nobuffer.data.dao.BookmarkDao
import com.prime.nobuffer.data.dao.CustomFilterRuleDao
import com.prime.nobuffer.data.dao.HistoryDao
import com.prime.nobuffer.data.dao.SiteCosmeticRuleDao
import com.prime.nobuffer.data.dao.SitePermissionGrantDao
import com.prime.nobuffer.data.dao.SiteShieldOverrideDao
import com.prime.nobuffer.data.dao.TabDao
import com.prime.nobuffer.data.entity.Bookmark
import com.prime.nobuffer.data.entity.CustomFilterRule
import com.prime.nobuffer.data.entity.CustomFilterRuleType
import com.prime.nobuffer.data.entity.HistoryEntry
import com.prime.nobuffer.data.entity.PermissionType
import com.prime.nobuffer.data.entity.SiteCosmeticRule
import com.prime.nobuffer.data.entity.SitePermissionGrant
import com.prime.nobuffer.data.entity.SiteShieldOverride
import com.prime.nobuffer.data.entity.TabEntity

@Database(
    entities = [
        HistoryEntry::class, Bookmark::class, TabEntity::class,
        SiteShieldOverride::class, SiteCosmeticRule::class,
        CustomFilterRule::class, SitePermissionGrant::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(BrowserDatabaseConverters::class)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tabDao(): TabDao
    abstract fun siteShieldOverrideDao(): SiteShieldOverrideDao
    abstract fun siteCosmeticRuleDao(): SiteCosmeticRuleDao
    abstract fun customFilterRuleDao(): CustomFilterRuleDao
    abstract fun sitePermissionGrantDao(): SitePermissionGrantDao
}

class BrowserDatabaseConverters {
    @TypeConverter
    fun fromFilterRuleType(value: CustomFilterRuleType): String = value.name

    @TypeConverter
    fun toFilterRuleType(value: String): CustomFilterRuleType = CustomFilterRuleType.valueOf(value)

    @TypeConverter
    fun fromPermissionType(value: PermissionType): String = value.name

    @TypeConverter
    fun toPermissionType(value: String): PermissionType = PermissionType.valueOf(value)
}
