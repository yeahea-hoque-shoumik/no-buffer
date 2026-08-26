package com.prime.nobuffer

import android.app.Application
import androidx.room.Room
import com.prime.nobuffer.data.BrowserDatabase
import com.prime.nobuffer.data.BrowserRepository

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
