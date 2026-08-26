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

    fun addSite(url: String, title: String) {
        if (url.isBlank() || url == "about:blank") return
        val host = try {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
        val label = title.ifBlank { host }
        val current = sites.value
        if (current.any { it.url == url }) return
        val palette = listOf(
            0xFF7B6EF5.toInt(), 0xFF36C9B0.toInt(), 0xFFE8A33D.toInt(),
            0xFFE84C4C.toInt(), 0xFF9994B8.toInt(), 0xFF6B5EE4.toInt()
        )
        val color = palette[Math.floorMod(url.hashCode(), palette.size)]
        update(current + QuickAccessSite(label = label, url = url, colorArgb = color))
    }
}
