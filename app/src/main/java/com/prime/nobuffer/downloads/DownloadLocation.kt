package com.prime.nobuffer.downloads

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment

object DownloadLocation {

    fun apply(request: DownloadManager.Request, fileName: String, downloadsLocation: String) {
        val raw = downloadsLocation.trim().ifBlank { "Downloads" }
        if (raw.startsWith("content://", ignoreCase = true)) {
            if (trySetDestinationUri(request, raw, fileName)) return
            val fallback = Uri.parse(raw).lastPathSegment?.let { sanitizeFolder(it) }
            applyPublicDir(request, fileName, fallback ?: "Downloads")
            return
        }
        applyPublicDir(request, fileName, raw)
    }

    private fun trySetDestinationUri(
        request: DownloadManager.Request,
        location: String,
        fileName: String
    ): Boolean {
        return try {
            val base = Uri.parse(location)
            // SAF tree URIs need DocumentsContract to create a document; DownloadManager
            // cannot write to them directly.
            if (base.toString().contains("/tree/")) return false
            val dest = if (base.lastPathSegment == fileName) {
                base
            } else {
                Uri.withAppendedPath(base, fileName)
            }
            request.setDestinationUri(dest)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun applyPublicDir(request: DownloadManager.Request, fileName: String, folder: String) {
        val safeFolder = sanitizeFolder(folder)
        val relative = if (safeFolder.isEmpty() || safeFolder.equals("Downloads", ignoreCase = true)) {
            fileName
        } else {
            "$safeFolder/$fileName"
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, relative)
    }

    fun sanitizeFolder(name: String): String =
        name.trim()
            .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
            .replace("..", "_")
            .trim('.', ' ', '_')
            .take(64)
}
