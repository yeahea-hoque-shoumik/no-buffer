package com.prime.nobuffer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion
import org.json.JSONObject
import org.json.JSONTokener

data class ReaderArticle(val title: String, val body: String)

object ReaderMode {
    const val EXTRACT_ARTICLE_JS = """
        (function() {
            try {
                var el = document.querySelector('article, main, [role="article"]') || document.body;
                var h1 = document.querySelector('h1');
                var title = (h1 && h1.innerText) ? h1.innerText.trim() : (document.title || '');
                var text = el ? (el.innerText || '') : '';
                return JSON.stringify({ title: title, text: text });
            } catch (e) {
                return JSON.stringify({
                    title: document.title || '',
                    text: (document.body && document.body.innerText) || ''
                });
            }
        })();
    """

    fun parse(raw: String?): ReaderArticle? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val decoded = try {
            JSONTokener(raw).nextValue()
        } catch (_: Exception) {
            raw
        }
        val json = when (decoded) {
            is JSONObject -> decoded
            is String -> try {
                JSONObject(decoded)
            } catch (_: Exception) {
                return null
            }
            else -> return null
        }
        val title = json.optString("title").trim()
        val body = json.optString("text").trim()
        if (title.isEmpty() && body.isEmpty()) return null
        return ReaderArticle(title = title.ifBlank { "Article" }, body = body)
    }
}

@Composable
fun ReaderModeScreen(
    article: ReaderArticle,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reader",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Close", color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        SelectionContainer {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = article.title,
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(16.dp))
                Text(
                    text = article.body,
                    color = colors.text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                Box(modifier = Modifier.size(24.dp))
            }
        }

        HomeIndicator()
    }
}
