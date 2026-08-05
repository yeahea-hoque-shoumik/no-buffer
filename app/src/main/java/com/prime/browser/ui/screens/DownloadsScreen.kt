package com.prime.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.browser.downloads.DownloadItem
import com.prime.browser.downloads.DownloadsViewModel
import com.prime.browser.ui.components.HomeIndicator
import com.prime.browser.ui.components.StatusBar
import com.prime.browser.ui.theme.Orion

private const val STORAGE_CAP_BYTES = 2L * 1024 * 1024 * 1024

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel()
) {
    val colors = Orion.colors
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    val usedBytes = downloads.sumOf { it.downloadedBytes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads",
                color = colors.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Manage",
                color = colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { selectionMode = !selectionMode }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Storage Used", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    text = "${formatSize(usedBytes)} / 2 GB",
                    color = colors.textMid,
                    fontSize = 13.sp
                )
            }
            Box(modifier = Modifier.size(10.dp))
            val fraction = (usedBytes.toFloat() / STORAGE_CAP_BYTES).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(Brush.horizontalGradient(listOf(colors.accent, colors.teal)), RoundedCornerShape(3.dp))
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(downloads, key = { it.id }) { item ->
                FileCard(
                    item = item,
                    selected = selectedIds.contains(item.id),
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            selectedIds = if (selectedIds.contains(item.id)) selectedIds - item.id else selectedIds + item.id
                        }
                    },
                    onLongClick = {
                        selectionMode = true
                        selectedIds = selectedIds + item.id
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (selectionMode && selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selectedIds.size} selected", color = colors.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    text = "Delete",
                    color = Color(0xFFE84C4C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        selectedIds.forEach { viewModel.deleteDownload(it) }
                        selectedIds = emptySet()
                        selectionMode = false
                    }
                )
            }
        }

        HomeIndicator()
    }
}

@Composable
private fun FileCard(
    item: DownloadItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val extColor = remember(item.extension) { colorForExtension(item.extension) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(extColor.copy(alpha = 0.18f))
                .border(1.dp, extColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.extension, color = extColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }

        Box(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.isRunning) {
                Box(modifier = Modifier.size(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.progressPercent / 100f).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(colors.accent, colors.teal)), RoundedCornerShape(2.dp))
                    )
                }
                Text(
                    text = "${item.progressPercent}% · ${formatSize(item.downloadedBytes)}",
                    color = colors.textDim,
                    fontSize = 11.sp
                )
            } else {
                Text(text = formatSize(item.totalBytes), color = colors.textDim, fontSize = 12.sp)
            }
        }

        Box(modifier = Modifier.size(8.dp))

        if (item.isSuccessful) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.teal.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = colors.teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else if (item.isRunning) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(colors.accent, RoundedCornerShape(2.dp)))
            }
        }
    }
}

private fun colorForExtension(ext: String): Color {
    val palette = listOf(
        Color(0xFF7B6EF5), Color(0xFF36C9B0), Color(0xFFE8A33D), Color(0xFFE84C4C)
    )
    return palette[Math.floorMod(ext.hashCode(), palette.size)]
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
