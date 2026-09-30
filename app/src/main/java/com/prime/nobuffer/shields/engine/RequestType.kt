package com.prime.nobuffer.shields.engine

/** Filter-list resource types as a bitmask, plus a best-effort resolver — Android's WebResourceRequest carries no type. */
object RequestType {
    const val SCRIPT = 1
    const val IMAGE = 2
    const val STYLESHEET = 4
    const val XHR = 8
    const val SUBDOCUMENT = 16
    const val MEDIA = 32
    const val FONT = 64
    const val OTHER = 128
    const val ALL = 255

    fun fromOptionName(name: String): Int? = when (name) {
        "script" -> SCRIPT
        "image" -> IMAGE
        "stylesheet", "css" -> STYLESHEET
        "xmlhttprequest", "xhr" -> XHR
        "subdocument", "frame" -> SUBDOCUMENT
        "media" -> MEDIA
        "font" -> FONT
        "other", "ping", "websocket", "object", "object-subrequest" -> OTHER
        else -> null
    }

    /** [headers] may be in any case; [urlLower] must already be lower-case. */
    fun resolve(urlLower: String, headers: Map<String, String>, isMainFrame: Boolean): Int {
        if (isMainFrame) return SUBDOCUMENT
        extensionType(urlLower)?.let { return it }

        val h = headers.entries.associate { it.key.lowercase() to it.value.lowercase() }
        when (h["sec-fetch-dest"]) {
            "script", "worker", "sharedworker", "serviceworker" -> return SCRIPT
            "image" -> return IMAGE
            "style" -> return STYLESHEET
            "font" -> return FONT
            "iframe", "frame" -> return SUBDOCUMENT
            "audio", "video", "track" -> return MEDIA
            "empty" -> return XHR
        }
        val accept = h["accept"].orEmpty()
        return when {
            accept.startsWith("image/") -> IMAGE
            accept.startsWith("text/css") -> STYLESHEET
            accept.startsWith("text/html") -> SUBDOCUMENT
            "x-requested-with" in h || "json" in accept -> XHR
            else -> OTHER
        }
    }

    private fun extensionType(urlLower: String): Int? {
        val end = urlLower.indexOfAny(charArrayOf('?', '#')).let { if (it == -1) urlLower.length else it }
        val path = urlLower.substring(0, end)
        val slash = path.lastIndexOf('/')
        val dot = path.lastIndexOf('.')
        if (dot == -1 || dot < slash) return null
        return when (path.substring(dot + 1)) {
            "js", "mjs" -> SCRIPT
            "css" -> STYLESHEET
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif", "bmp" -> IMAGE
            "woff", "woff2", "ttf", "otf", "eot" -> FONT
            "mp4", "webm", "m3u8", "mpd", "mp3", "ogg", "m4a", "m4s" -> MEDIA
            "html", "htm" -> SUBDOCUMENT
            else -> null
        }
    }
}
