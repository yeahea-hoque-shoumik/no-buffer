package com.prime.nobuffer.reader

data class ReaderArticle(
    val title: String,
    val byline: String?,
    val contentHtml: String
)

object ReaderMode {

    /**
     * Document-start-safe article extractor. Returns JSON
     * `{title, byline, contentHtml}` via `JSON.stringify`.
     * Does not throw if `document.body` is missing.
     */
    const val EXTRACT_ARTICLE_JS: String = """(function() {
    function textOf(el) {
        if (!el) return '';
        var attr = '';
        try { attr = (el.getAttribute && el.getAttribute('content')) || ''; } catch (e) {}
        return (attr || el.innerText || el.textContent || '').trim();
    }
    var article = document.querySelector('article')
        || document.querySelector('[role="article"]')
        || document.querySelector('main')
        || document.querySelector('.post-content, .entry-content, .article-body, .article-content, #article, #content');
    var titleEl = document.querySelector('meta[property="og:title"]')
        || document.querySelector('h1')
        || document.querySelector('title');
    var title = textOf(titleEl) || document.title || '';
    var bylineEl = document.querySelector('meta[name="author"]')
        || document.querySelector('[rel="author"]')
        || document.querySelector('[itemprop="author"]')
        || document.querySelector('.byline, .author, .article-byline');
    var byline = textOf(bylineEl);
    var contentHtml = article ? (article.innerHTML || '') : '';
    return JSON.stringify({ title: title, byline: byline, contentHtml: contentHtml });
})();"""

    /**
     * Parses the result of `WebView.evaluateJavascript(EXTRACT_ARTICLE_JS)`.
     * Handles both raw JSON objects and the extra-quoted JSON string WebView wraps
     * around JS string return values.
     */
    fun parseExtractResult(rawJsResult: String): ReaderArticle? {
        val jsonText = unwrapJsResult(rawJsResult) ?: return null
        val fields = parseJsonObjectStrings(jsonText) ?: return null
        return ReaderArticle(
            title = fields["title"] ?: "",
            byline = fields["byline"]?.takeIf { it.isNotEmpty() },
            contentHtml = fields["contentHtml"] ?: ""
        )
    }

    private fun unwrapJsResult(rawJsResult: String): String? {
        val trimmed = rawJsResult.trim()
        if (trimmed.isEmpty() || trimmed == "null" || trimmed == "undefined" || trimmed == "\"null\"") {
            return null
        }
        if (trimmed.startsWith("{")) return trimmed
        if (trimmed.startsWith("\"")) return decodeJsonString(trimmed)
        return null
    }

    /** Parses a flat JSON object whose values are strings (or null). */
    private fun parseJsonObjectStrings(json: String): Map<String, String>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        var i = 0
        val s = inner
        while (i < s.length) {
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length) break
            if (s[i] != '"') return null
            val keyEnd = findClosingQuote(s, i) ?: return null
            val key = decodeJsonString(s.substring(i, keyEnd + 1)) ?: return null
            i = keyEnd + 1
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length || s[i] != ':') return null
            i++
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length) return null
            val value = when {
                s[i] == '"' -> {
                    val valueEnd = findClosingQuote(s, i) ?: return null
                    val v = decodeJsonString(s.substring(i, valueEnd + 1)) ?: return null
                    i = valueEnd + 1
                    v
                }
                s.startsWith("null", i) -> {
                    i += 4
                    ""
                }
                else -> return null
            }
            result[key] = value
            while (i < s.length && s[i].isWhitespace()) i++
            if (i < s.length && s[i] == ',') i++
        }
        return result
    }

    private fun findClosingQuote(s: String, openIndex: Int): Int? {
        var i = openIndex + 1
        while (i < s.length) {
            when (s[i]) {
                '\\' -> i += 2
                '"' -> return i
                else -> i++
            }
        }
        return null
    }

    private fun decodeJsonString(quoted: String): String? {
        if (quoted.length < 2 || quoted.first() != '"' || quoted.last() != '"') return null
        val sb = StringBuilder(quoted.length)
        var i = 1
        val end = quoted.length - 1
        while (i < end) {
            val c = quoted[i]
            if (c == '\\' && i + 1 < end) {
                when (val n = quoted[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\u0008')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 6 > end) return null
                        val cp = quoted.substring(i + 2, i + 6).toIntOrNull(16) ?: return null
                        sb.append(cp.toChar())
                        i += 6
                        continue
                    }
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
