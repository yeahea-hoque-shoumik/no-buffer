package com.prime.nobuffer

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prime.nobuffer.blocklist.AppLockController
import com.prime.nobuffer.blocklist.AppLockStore
import com.prime.nobuffer.blocklist.LockPasswordStore
import com.prime.nobuffer.blocklist.GamblingBlocklist
import com.prime.nobuffer.blocklist.SiteBlocker
import com.prime.nobuffer.data.BrowserDatabase
import com.prime.nobuffer.data.BrowserRepository
import com.prime.nobuffer.settings.SettingsRepository
import com.prime.nobuffer.shields.AdTrackerBlocklist
import com.prime.nobuffer.shields.CosmeticRuleStore
import com.prime.nobuffer.shields.ShieldsResolver
import com.prime.nobuffer.shields.engine.FilterEngineManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BrowserApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: BrowserDatabase by lazy {
        Room.databaseBuilder(this, BrowserDatabase::class.java, "browser.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val repository: BrowserRepository by lazy {
        BrowserRepository(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val adTrackerBlocklist: AdTrackerBlocklist by lazy {
        AdTrackerBlocklist(this)
    }

    val filterEngines: FilterEngineManager by lazy {
        FilterEngineManager(this, applicationScope)
    }

    val cosmeticRuleStore: CosmeticRuleStore by lazy {
        CosmeticRuleStore(repository, applicationScope)
    }

    val shieldsResolver: ShieldsResolver by lazy {
        ShieldsResolver(settingsRepository, repository, applicationScope)
    }

    val lockPasswordStore: LockPasswordStore by lazy { LockPasswordStore(this) }

    val gamblingBlocklist: GamblingBlocklist by lazy {
        GamblingBlocklist { assets.open("blocklists/gambling_hosts.txt") }
    }

    val siteBlocker: SiteBlocker by lazy {
        SiteBlocker(repository, lockPasswordStore, applicationScope, gamblingBlocklist)
    }

    val appLockController: AppLockController by lazy {
        AppLockController(AppLockStore(this), lockPasswordStore)
    }

    override fun onCreate() {
        super.onCreate()
        siteBlocker
        applicationScope.launch(Dispatchers.Default) { gamblingBlocklist.warmUp() }
        // Phase 21: purge expired per-site permission grants on cold start.
        applicationScope.launch { repository.purgeExpiredPermissionGrants() }
        // Ad-block engines: load bundled/cached lists, then refresh from the network when stale.
        filterEngines.loadAsync()
        applicationScope.launch {
            val last = settingsRepository.settings.first().blocklistLastUpdated
            filterEngines.refreshIfStale(last)?.let { settingsRepository.setBlocklistLastUpdated(it) }
        }
    }

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS blocked_sites (" +
                        "host TEXT NOT NULL PRIMARY KEY, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN unlockUntil INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN dailyBudgetMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN usedMillisToday INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN budgetDayEpoch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN scheduleStartMinute INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN scheduleEndMinute INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN scheduleDaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE blocked_sites ADD COLUMN videoAllowed INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
