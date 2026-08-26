package com.prime.nobuffer.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.nobuffer.data.entity.HistoryEntry
import com.prime.nobuffer.history.HistoryViewModel
import com.prime.nobuffer.ui.components.ClearBrowsingDataDialog
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val colors = Orion.colors
    val grouped by viewModel.groupedHistory.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                    text = "History",
                    color = colors.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear All",
                    color = colors.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showClearDialog = true }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = colors.textDim, fontSize = 16.sp)
                Box(modifier = Modifier.size(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it; viewModel.filter(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search history", color = colors.textDim, fontSize = 14.sp)
                        }
                        inner()
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                grouped.forEach { (label, entries) ->
                    item(key = "label-$label") {
                        Text(
                            text = label.uppercase(),
                            color = colors.textDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
                        )
                    }
                    item(key = "group-$label") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        ) {
                            entries.forEachIndexed { index, entry ->
                                SwipeableHistoryRow(
                                    entry = entry,
                                    showDivider = index != entries.lastIndex,
                                    selectionMode = selectionMode,
                                    selected = selectedIds.contains(entry.id),
                                    onClick = {
                                        if (selectionMode) {
                                            selectedIds = if (selectedIds.contains(entry.id)) {
                                                selectedIds - entry.id
                                            } else {
                                                selectedIds + entry.id
                                            }
                                        } else {
                                            onNavigate(entry.url)
                                        }
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selectedIds = selectedIds + entry.id
                                    },
                                    onDelete = {
                                        viewModel.delete(entry)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted \"${entry.title.ifBlank { entry.url }}\"",
                                                actionLabel = "Undo"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete(entry)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} selected",
                        color = colors.text,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Delete",
                        color = Color(0xFFE84C4C),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val allEntries = grouped.values.flatten()
                            viewModel.deleteAll(allEntries.filter { selectedIds.contains(it.id) })
                            selectionMode = false
                            selectedIds = emptySet()
                        }
                    )
                    Box(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Cancel",
                        color = colors.textMid,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectionMode = false
                            selectedIds = emptySet()
                        }
                    )
                }
            }

            HomeIndicator()
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showClearDialog) {
        ClearBrowsingDataDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = { clearHistory, clearCookies, clearCache, range ->
                viewModel.clearBrowsingData(clearHistory, clearCookies, clearCache, range)
                showClearDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHistoryRow(
    entry: HistoryEntry,
    showDivider: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = Orion.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE84C4C).copy(alpha = 0.15f))
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Delete", color = Color(0xFFE84C4C), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        Column(modifier = Modifier.background(colors.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = remember(entry.url) { colorForUrl(entry.url) }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(dotColor.copy(alpha = 0.18f))
                        .border(1.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
                }

                Box(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title.ifBlank { entry.url },
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.url,
                        color = colors.textDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(modifier = Modifier.size(8.dp))

                Text(
                    text = timeOfDay(entry.visitedAt),
                    color = colors.textDim,
                    fontSize = 11.sp
                )
            }
            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )
            }
        }
    }
}

private fun colorForUrl(url: String): Color {
    val palette = listOf(
        Color(0xFF7B6EF5), Color(0xFF36C9B0), Color(0xFFE8A33D),
        Color(0xFFE84C4C), Color(0xFF6B5EE4), Color(0xFF9994B8)
    )
    return palette[Math.floorMod(url.hashCode(), palette.size)]
}

private fun timeOfDay(timestamp: Long): String {
    return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
