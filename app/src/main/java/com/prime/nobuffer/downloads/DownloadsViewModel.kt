package com.prime.nobuffer.downloads

import android.app.Application
import android.app.DownloadManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: Int
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

    val isRunning: Boolean
        get() = status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING

    val isSuccessful: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val extension: String
        get() = fileName.substringAfterLast('.', "FILE").take(4).uppercase()
}

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = application.getSystemService(Application.DOWNLOAD_SERVICE) as DownloadManager

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _downloads.value = queryDownloads()
                delay(1000)
            }
        }
    }

    fun deleteDownload(id: Long) {
        downloadManager.remove(id)
    }

    private fun queryDownloads(): List<DownloadItem> {
        val cursor = downloadManager.query(DownloadManager.Query()) ?: return emptyList()
        val result = mutableListOf<DownloadItem>()
        cursor.use {
            val idIdx = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIdx = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val totalIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedIdx = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            while (it.moveToNext()) {
                result.add(
                    DownloadItem(
                        id = it.getLong(idIdx),
                        fileName = it.getString(titleIdx) ?: "Download",
                        totalBytes = it.getLong(totalIdx),
                        downloadedBytes = it.getLong(downloadedIdx),
                        status = it.getInt(statusIdx)
                    )
                )
            }
        }
        return result.sortedByDescending { it.id }
    }
}
