package com.prime.nobuffer

import android.app.Application
import androidx.room.Room
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

    override fun onCreate() {
        super.onCreate()
        // Phase 21: purge expired per-site permission grants on cold start.
        applicationScope.launch { repository.purgeExpiredPermissionGrants() }
    }
}
