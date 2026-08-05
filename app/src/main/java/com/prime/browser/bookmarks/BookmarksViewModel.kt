package com.prime.browser.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.browser.BrowserApplication
import com.prime.browser.data.entity.Bookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookmarkNode(val bookmark: Bookmark, val depth: Int, val hasChildren: Boolean)

class BookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BrowserApplication).repository

    private val expandedFolders = MutableStateFlow<Set<Long>>(emptySet())
    private val query = MutableStateFlow("")

    private val allBookmarks: StateFlow<List<Bookmark>> = repository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayList: StateFlow<List<BookmarkNode>> = combine(allBookmarks, expandedFolders, query) { all, expanded, q ->
        if (q.isNotBlank()) {
            all.filter { !it.isFolder && (it.title.contains(q, true) || it.url.contains(q, true)) }
                .map { BookmarkNode(it, 0, false) }
        } else {
            buildTree(all, parentId = null, depth = 0, expanded = expanded)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Bookmark>> = allBookmarks
        .map { all -> all.filter { it.isFolder } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildTree(all: List<Bookmark>, parentId: Long?, depth: Int, expanded: Set<Long>): List<BookmarkNode> {
        val children = all.filter { it.parentId == parentId }.sortedBy { it.sortOrder }
        return children.flatMap { b ->
            val hasChildren = b.isFolder && all.any { it.parentId == b.id }
            val node = BookmarkNode(b, depth, hasChildren)
            if (b.isFolder && expanded.contains(b.id)) {
                listOf(node) + buildTree(all, b.id, depth + 1, expanded)
            } else {
                listOf(node)
            }
        }
    }

    fun toggleFolder(id: Long) {
        expandedFolders.value = if (expandedFolders.value.contains(id)) {
            expandedFolders.value - id
        } else {
            expandedFolders.value + id
        }
    }

    fun filter(q: String) {
        query.value = q
    }

    fun addBookmark(url: String, title: String, parentId: Long? = null) {
        viewModelScope.launch { repository.insertBookmark(Bookmark(url = url, title = title, parentId = parentId)) }
    }

    fun addFolder(title: String, parentId: Long? = null) {
        viewModelScope.launch { repository.insertBookmark(Bookmark(url = "", title = title, isFolder = true, parentId = parentId)) }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { repository.deleteBookmark(bookmark.id) }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch { repository.updateBookmark(bookmark) }
    }

    fun moveBookmark(bookmark: Bookmark, newParentId: Long?) {
        viewModelScope.launch { repository.updateBookmark(bookmark.copy(parentId = newParentId)) }
    }

    fun reorderWithinParent(bookmark: Bookmark, direction: Int) {
        val siblings = allBookmarks.value
            .filter { it.parentId == bookmark.parentId }
            .sortedBy { it.sortOrder }
        val index = siblings.indexOfFirst { it.id == bookmark.id }
        val targetIndex = index + direction
        if (index == -1 || targetIndex !in siblings.indices) return

        val current = siblings[index]
        val target = siblings[targetIndex]
        viewModelScope.launch {
            repository.updateBookmark(current.copy(sortOrder = target.sortOrder))
            repository.updateBookmark(target.copy(sortOrder = current.sortOrder))
        }
    }
}
