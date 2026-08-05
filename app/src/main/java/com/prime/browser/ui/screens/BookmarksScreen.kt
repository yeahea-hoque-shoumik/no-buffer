package com.prime.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.browser.bookmarks.BookmarkNode
import com.prime.browser.bookmarks.BookmarksViewModel
import com.prime.browser.data.entity.Bookmark
import com.prime.browser.ui.components.HomeIndicator
import com.prime.browser.ui.components.StatusBar
import com.prime.browser.ui.theme.Orion

@Composable
fun BookmarksScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = viewModel()
) {
    val colors = Orion.colors
    val nodes by viewModel.displayList.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Bookmark?>(null) }

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
                    text = "Bookmarks",
                    color = colors.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
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
                            Text("Search bookmarks", color = colors.textDim, fontSize = 14.sp)
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
                items(nodes, key = { it.bookmark.id }) { node ->
                    BookmarkRow(
                        node = node,
                        onClick = {
                            if (node.bookmark.isFolder) {
                                viewModel.toggleFolder(node.bookmark.id)
                            } else {
                                onNavigate(node.bookmark.url)
                            }
                        },
                        onOpenInNewTab = { onNavigate(node.bookmark.url) },
                        onEdit = { viewModel.updateBookmark(node.bookmark.copy(title = node.bookmark.title)) },
                        onDelete = { viewModel.deleteBookmark(node.bookmark) },
                        onMove = { moveTarget = node.bookmark },
                        onMoveUp = { viewModel.reorderWithinParent(node.bookmark, -1) },
                        onMoveDown = { viewModel.reorderWithinParent(node.bookmark, 1) }
                    )
                }
            }

            HomeIndicator()
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = colors.accent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 48.dp)
        ) {
            Text("+ Bookmark", color = Orion.colors.text, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 10.dp))
        }
    }

    if (showAddDialog) {
        AddBookmarkDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url, title ->
                viewModel.addBookmark(url, title)
                showAddDialog = false
            }
        )
    }

    moveTarget?.let { bookmark ->
        MoveBookmarkDialog(
            folders = folders.filter { it.id != bookmark.id },
            onDismiss = { moveTarget = null },
            onSelect = { parentId ->
                viewModel.moveBookmark(bookmark, parentId)
                moveTarget = null
            }
        )
    }
}

@Composable
private fun BookmarkRow(
    node: BookmarkNode,
    onClick: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = Orion.colors
    var expanded by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 16).dp, bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = { expanded = true }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⠿",
            color = colors.textDim,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(end = 8.dp)
                .pointerInput(node.bookmark.id) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccum += dragAmount
                            if (dragAccum > 60f) {
                                onMoveDown()
                                dragAccum = 0f
                            } else if (dragAccum < -60f) {
                                onMoveUp()
                                dragAccum = 0f
                            }
                        },
                        onDragEnd = { dragAccum = 0f }
                    )
                }
        )

        if (node.bookmark.isFolder) {
            Text(if (node.hasChildren) "📁" else "📂", fontSize = 16.sp)
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(8.dp).background(colors.accent, CircleShape))
            }
        }

        Box(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.bookmark.title.ifBlank { node.bookmark.url },
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!node.bookmark.isFolder) {
                Text(
                    text = node.bookmark.url,
                    color = colors.textDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (!node.bookmark.isFolder) {
                DropdownMenuItem(text = { Text("Open") }, onClick = { expanded = false; onClick() })
                DropdownMenuItem(text = { Text("Open in New Tab") }, onClick = { expanded = false; onOpenInNewTab() })
            }
            DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(text = { Text("Move") }, onClick = { expanded = false; onMove() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}

@Composable
private fun AddBookmarkDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    val colors = Orion.colors
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Add Bookmark", color = colors.text) },
        text = {
            Column {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (title.isEmpty()) Text("Title", color = colors.textDim, fontSize = 14.sp)
                        inner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (url.isEmpty()) Text("URL", color = colors.textDim, fontSize = 14.sp)
                        inner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (url.isNotBlank()) onConfirm(url, title.ifBlank { url }) }) {
                Text("Add", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) }
        }
    )
}

@Composable
private fun MoveBookmarkDialog(folders: List<Bookmark>, onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    val colors = Orion.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Move to", color = colors.text) },
        text = {
            Column {
                Text(
                    text = "Top Level",
                    color = colors.text,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onSelect(null)
                        }
                        .padding(vertical = 10.dp)
                )
                folders.forEach { folder ->
                    Text(
                        text = folder.title,
                        color = colors.text,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onSelect(folder.id)
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) }
        }
    )
}
