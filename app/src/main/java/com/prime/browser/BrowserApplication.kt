package com.prime.browser

import android.app.Application
import androidx.room.Room
import com.prime.browser.data.BrowserDatabase
import com.prime.browser.data.BrowserRepository

class BrowserApplication : Application() {

    val database: BrowserDatabase by lazy {
        Room.databaseBuilder(this, BrowserDatabase::class.java, "browser.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val repository: BrowserRepository by lazy {
        BrowserRepository(database)
    }
}
