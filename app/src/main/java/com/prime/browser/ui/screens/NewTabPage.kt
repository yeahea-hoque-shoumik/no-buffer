package com.prime.browser.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.browser.newtab.QuickAccessSite
import com.prime.browser.newtab.QuickAccessViewModel
import com.prime.browser.ui.theme.Orion

/**
 * Home content only — no StatusBar/PillBar/HomeIndicator. Meant to be overlaid inside
 * BrowserScreen's content area so the pill bar never remounts when navigating home <-> a page.
 */
@Composable
fun NewTabContent(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickAccessViewModel = viewModel()
) {
    val colors = Orion.colors
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    var editingSite by remember { mutableStateOf<QuickAccessSite?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(listOf(colors.accentGlow, Color.Transparent)),
                    CircleShape
                )
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)) {
            SectionLabel("QUICK ACCESS")
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(sites, key = { it.id }) { site ->
                    QuickAccessTile(
                        site = site,
                        onClick = { onNavigate(site.url) },
                        onRemove = { viewModel.update(sites.filterNot { it.id == site.id }) },
                        onEdit = { editingSite = site }
                    )
                }
            }
        }
    }

    editingSite?.let { site ->
        EditSiteDialog(
            site = site,
            onDismiss = { editingSite = null },
            onConfirm = { newLabel, newUrl ->
                viewModel.update(sites.map { if (it.id == site.id) it.copy(label = newLabel, url = newUrl) else it })
                editingSite = null
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Orion.colors.textDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun QuickAccessTile(site: QuickAccessSite, onClick: () -> Unit, onRemove: () -> Unit, onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(site.colorArgb)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = { expanded = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = site.label.take(1).uppercase(),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Open") }, onClick = { expanded = false; onClick() })
                DropdownMenuItem(text = { Text("Open in New Tab") }, onClick = { expanded = false; onClick() })
                DropdownMenuItem(text = { Text("Remove") }, onClick = { expanded = false; onRemove() })
                DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
            }
        }
        Box(modifier = Modifier.size(6.dp))
        Text(
            text = site.label,
            color = Orion.colors.textMid,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditSiteDialog(
    site: QuickAccessSite,
    onDismiss: () -> Unit,
    onConfirm: (label: String, url: String) -> Unit
) {
    val colors = Orion.colors
    var label by remember { mutableStateOf(site.label) }
    var url by remember { mutableStateOf(site.url) }

    val normalizedUrl = normalizeUrl(url)
    val isUrlValid = isValidUrl(normalizedUrl)
    val showError = url.isNotBlank() && !isUrlValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Edit Shortcut", color = colors.text) },
        text = {
            Column {
                Text("Name", color = colors.textDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                BasicTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                Text("URL", color = colors.textDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = "Enter a valid URL",
                        color = Color(0xFFE84C4C),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && isUrlValid,
                onClick = { onConfirm(label.trim(), normalizedUrl) }
            ) {
                Text("Save", color = if (label.isNotBlank() && isUrlValid) colors.accent else colors.textDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) }
        }
    )
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.isBlank() || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun isValidUrl(url: String): Boolean =
    url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
