package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.prime.nobuffer.tabs.BrowserTab
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion
import com.prime.nobuffer.ui.theme.OrionTokens
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkCardColors = OrionTokens.Incognito

@Composable
fun TabSwitcherScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (BrowserTab) -> Unit,
    onCloseTab: (BrowserTab) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit = {},
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val regularTabs = tabs.filter { !it.isIncognito }
    val incognitoTabs = tabs.filter { it.isIncognito }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${regularTabs.size} Tabs",
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
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNewTab)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text("+ New", color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(regularTabs, key = { it.id }) { tab ->
                TabCard(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onSelectTab(tab) },
                    onClose = { onCloseTab(tab) },
                    modifier = Modifier.padding(6.dp)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                NewTabSlot(onClick = onNewTab, modifier = Modifier.padding(6.dp))
            }

            if (incognitoTabs.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "PRIVATE",
                        color = colors.textDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 6.dp)
                    )
                }
                items(incognitoTabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isActive = tab.id == activeTabId,
                        onClick = { onSelectTab(tab) },
                        onClose = { onCloseTab(tab) },
                        forceDark = true,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.pill)
                .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Done",
                color = colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            )
            Text("⊞", color = colors.textMid, fontSize = 16.sp)
            Box(modifier = Modifier.size(24.dp))
            Text(
                text = "Private",
                color = colors.textMid,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNewPrivateTab
                )
            )
        }

        HomeIndicator()
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    forceDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = if (forceDark) DarkCardColors else Orion.colors
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dotColor = remember(tab.id) { colorForTab(tab.url) }

    Box(
        modifier = modifier
            .height(168.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX
                alpha = 1f - (abs(offsetX) / 500f).coerceIn(0f, 1f)
            }
            .pointerInput(tab.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > 180f) onClose() else offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) colors.accent else colors.border,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.elevated)
                    .border(width = 0.dp, color = colors.border)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(dotColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(7.dp).background(dotColor, CircleShape))
                }
                Box(modifier = Modifier.size(8.dp))
                Text(
                    text = hostOf(tab.url),
                    color = colors.textMid,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = colors.textDim,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(colors.border, RoundedCornerShape(3.dp))
                )
                Box(modifier = Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .background(colors.border, RoundedCornerShape(3.dp))
                )
                Box(modifier = Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(dotColor.copy(alpha = 0.12f))
                        .border(1.dp, dotColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun NewTabSlot(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Orion.colors
    Box(
        modifier = modifier
            .height(168.dp)
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = colors.border,
                cornerRadius = CornerRadius(18.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.elevated),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = colors.textDim, fontSize = 20.sp)
        }
    }
}

private fun colorForTab(url: String): androidx.compose.ui.graphics.Color {
    val palette = listOf(
        androidx.compose.ui.graphics.Color(0xFF7B6EF5),
        androidx.compose.ui.graphics.Color(0xFF36C9B0),
        androidx.compose.ui.graphics.Color(0xFFE8A33D),
        androidx.compose.ui.graphics.Color(0xFFE84C4C)
    )
    return palette[Math.floorMod(url.hashCode(), palette.size)]
}

private fun hostOf(url: String): String {
    if (url.isBlank() || url == "about:blank") return "New Tab"
    return try {
        java.net.URI(url).host ?: url
    } catch (e: Exception) {
        url
    }
}
