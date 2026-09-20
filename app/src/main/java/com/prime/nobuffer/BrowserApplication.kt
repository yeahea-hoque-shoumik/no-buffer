package com.prime.nobuffer

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prime.nobuffer.blocklist.AppLockController
import com.prime.nobuffer.blocklist.AppLockStore
import com.prime.nobuffer.blocklist.LockPasswordStore
import com.prime.nobuffer.blocklist.SiteBlocker
import com.prime.nobuffer.data.BrowserDatabase
import com.prime.nobuffer.data.BrowserRepository
import com.prime.nobuffer.settings.SettingsRepository
import com.prime.nobuffer.shields.AdTrackerBlocklist
import com.prime.nobuffer.shields.CosmeticRuleStore
import com.prime.nobuffer.shields.ShieldsResolver
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

    val cosmeticRuleStore: CosmeticRuleStore by lazy {
        CosmeticRuleStore(repository, applicationScope)
    }

    val shieldsResolver: ShieldsResolver by lazy {
        ShieldsResolver(settingsRepository, repository, applicationScope)
    }

    val lockPasswordStore: LockPasswordStore by lazy { LockPasswordStore(this) }

    val siteBlocker: SiteBlocker by lazy {
        SiteBlocker(repository, lockPasswordStore, applicationScope)
    }

    val appLockController: AppLockController by lazy {
        AppLockController(AppLockStore(this), lockPasswordStore)
    }

    override fun onCreate() {
        super.onCreate()
        siteBlocker
        // Phase 21: purge expired per-site permission grants on cold start.
        applicationScope.launch { repository.purgeExpiredPermissionGrants() }
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
