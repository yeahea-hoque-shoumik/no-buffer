package com.prime.browser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.browser.tabs.TabCountBadge
import com.prime.browser.ui.theme.Orion
import java.net.URI

@Composable
fun PillBar(
    url: String?,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    tabCount: Int = 1,
    onBackClick: () -> Unit = {},
    onFieldClick: () -> Unit = {},
    onTabsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val colors = Orion.colors
    val isSearchMode = url.isNullOrBlank() || url == "about:blank"
    val isSecure = url?.startsWith("https://") == true
    val host = remember(url) { hostOf(url) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.pill, RoundedCornerShape(28.dp))
                .border(1.dp, colors.borderHi, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconTile(onClick = onBackClick) {
                    Text("‹", color = colors.textMid, fontSize = 18.sp)
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSearchMode) colors.accent.copy(alpha = 0.18f) else colors.elevated
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onFieldClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchMode) {
                    SearchGlyph(color = colors.accent)
                    Box(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Search or type URL",
                        color = colors.textDim,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    if (isSecure) {
                        LockGlyph(color = colors.teal)
                    } else {
                        WarningGlyph(color = androidx.compose.ui.graphics.Color(0xFFE8A33D))
                    }
                    Box(modifier = Modifier.size(6.dp))
                    Text(
                        text = host,
                        color = colors.textMid,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.size(6.dp))

            IconTile(onClick = onTabsClick) {
                TabCountBadge(count = tabCount, color = colors.textMid, size = 20.dp)
            }

            IconTile(onClick = onMenuClick) {
                Text("⋮", color = colors.textMid, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun IconTile(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SearchGlyph(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        drawCircle(color = color, radius = size.minDimension * 0.32f, style = Stroke(width = 1.6.dp.toPx()))
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.72f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.95f),
            strokeWidth = 1.8.dp.toPx()
        )
    }
}

@Composable
private fun LockGlyph(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(13.dp)) {
        val bodyTop = size.height * 0.42f
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, bodyTop),
            size = Size(size.width, size.height - bodyTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, 0f),
            size = Size(size.width * 0.64f, size.height * 0.7f),
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

@Composable
private fun WarningGlyph(color: androidx.compose.ui.graphics.Color) {
    Text("!", color = color, fontSize = 13.sp)
}

private fun hostOf(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return try {
        URI(url).host ?: url
    } catch (e: Exception) {
        url
    }
}
