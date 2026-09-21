package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.ui.theme.Orion

private data class MenuItem(val label: String, val value: String = "", val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuBottomSheet(
    url: String,
    textZoom: Int = 100,
    desktopSiteEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onInstall: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onAddToHomePage: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onFindOnPage: () -> Unit,
    onBlockElement: () -> Unit,
    onSettings: () -> Unit,
    onCopyLink: () -> Unit = {},
    onZoomDelta: (Int) -> Unit = {},
    onZoomSet: (Int) -> Unit = {},
    onBookmarks: () -> Unit = {},
    onRestoreRecentlyClosed: () -> Unit = {},
    onReaderMode: () -> Unit = {},
    onLockSite: () -> Unit = {},
    onSiteInfo: () -> Unit = {},
    onDesktopSite: () -> Unit = {}
) {
    val colors = Orion.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 16.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(colors.textDim, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒", color = colors.teal, fontSize = 13.sp)
                Box(modifier = Modifier.size(8.dp))
                Text(
                    text = url,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("↗", color = colors.textMid, fontSize = 15.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAction("↻", "Reload", onReload)
                QuickAction("🖨", "Print/PDF", onPrint)
                QuickAction("↗", "Share", onShare)
                QuickAction("⇩", "Install", onInstall)
            }

            ZoomRow(
                textZoom = textZoom,
                onZoomDelta = onZoomDelta,
                onZoomSet = onZoomSet
            )

            val items = listOf(
                MenuItem("New Tab", onClick = onNewTab),
                MenuItem("New Private Tab", onClick = onNewPrivateTab),
                MenuItem("Add to Home Page", onClick = onAddToHomePage),
                MenuItem("Bookmarks", onClick = onBookmarks),
                MenuItem("History", onClick = onHistory),
                MenuItem("Downloads", onClick = onDownloads),
                MenuItem("Recently closed", onClick = onRestoreRecentlyClosed),
                MenuItem("Copy link", onClick = onCopyLink),
                MenuItem("Find on Page", onClick = onFindOnPage),
                MenuItem("Reader mode", onClick = onReaderMode),
                MenuItem("Desktop site", if (desktopSiteEnabled) "On" else "Off", onDesktopSite),
                MenuItem("Lock this site", onClick = onLockSite),
                MenuItem("Site info", onClick = onSiteInfo),
                MenuItem("Block Element", onClick = onBlockElement),
                MenuItem("Settings", onClick = onSettings)
            )

            Column {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = item.onClick
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            color = colors.text,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.value.isNotEmpty()) {
                            Text(item.value, color = colors.textMid, fontSize = 13.sp)
                            Box(modifier = Modifier.size(8.dp))
                        }
                        Text("›", color = colors.textDim, fontSize = 16.sp)
                    }
                }
            }

            Box(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ZoomRow(
    textZoom: Int,
    onZoomDelta: (Int) -> Unit,
    onZoomSet: (Int) -> Unit
) {
    val colors = Orion.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Zoom: $textZoom%",
            color = colors.text,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        ZoomButton("−") { onZoomDelta(-10) }
        Box(modifier = Modifier.size(8.dp))
        ZoomButton("+") { onZoomDelta(10) }
        Box(modifier = Modifier.size(8.dp))
        if (textZoom != 100) {
            Text(
                text = "Reset",
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onZoomSet(100) }
                )
            )
        }
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    val colors = Orion.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickAction(glyph: String, label: String, onClick: () -> Unit) {
    val colors = Orion.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.elevated)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 20.sp, color = colors.text)
        }
        Box(modifier = Modifier.size(6.dp))
        Text(label, color = colors.textMid, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
