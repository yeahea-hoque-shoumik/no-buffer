package com.prime.nobuffer.newtab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickAccessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuickAccessRepository(application)

    val sites: StateFlow<List<QuickAccessSite>> = repository.sites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.defaultSites())

    fun update(newSites: List<QuickAccessSite>) {
        viewModelScope.launch { repository.save(newSites) }
    }
}
