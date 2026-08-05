package com.prime.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.browser.history.HistoryViewModel
import com.prime.browser.settings.DarkModeOption
import com.prime.browser.settings.SettingsViewModel
import com.prime.browser.ui.components.ClearBrowsingDataDialog
import com.prime.browser.ui.components.HomeIndicator
import com.prime.browser.ui.components.StatusBar
import com.prime.browser.ui.theme.Orion

@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit,
    onOpenSite: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val colors = Orion.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        item { StatusBar() }

        item {
            Text(
                text = "Settings",
                color = colors.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.22f), colors.teal.copy(alpha = 0.12f))))
                    .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.accent, colors.teal))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("O", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Orion User", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Not signed in", color = colors.textMid, fontSize = 12.sp)
                }
                Text("›", color = colors.textMid, fontSize = 18.sp)
            }
        }

        item {
            SettingsSection(title = "GENERAL") {
                SettingsRow(icon = "🔍", label = "Search Engine", value = settings.searchEngine) { showSearchEngineDialog = true }
                SettingsRow(
                    icon = "🏠",
                    label = "Homepage",
                    value = settings.homepageUrl.ifBlank { "New Tab Page" },
                    showDivider = false
                ) { showHomepageDialog = true }
            }
        }

        item {
            SettingsSection(title = "PRIVACY & SECURITY") {
                ToggleRow(icon = "🛡️", label = "Ad Blocker", checked = settings.adBlockerEnabled) { viewModel.setAdBlockerEnabled(it) }
                ToggleRow(icon = "🚫", label = "Do Not Track", checked = settings.doNotTrackEnabled) { viewModel.setDoNotTrackEnabled(it) }
                ToggleRow(icon = "🍪", label = "Block 3rd-party Cookies", checked = settings.blockThirdPartyCookies) { viewModel.setBlockThirdPartyCookies(it) }
                SettingsRow(icon = "🔒", label = "Privacy & Security", value = "", onClick = onOpenPrivacy)
                SettingsRow(icon = "🌐", label = "Site Settings", value = "", onClick = onOpenSite)
                SettingsRow(icon = "🧹", label = "Clear Browsing Data", value = "", showDivider = false, onClick = { showClearDataDialog = true })
            }
        }

        item {
            SettingsSection(title = "APPEARANCE") {
                SettingsRow(icon = "🎨", label = "Theme", value = settings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() }) { showThemeDialog = true }
                SettingsRow(icon = "🔤", label = "Font Size", value = "${settings.textZoom}%") { showZoomDialog = true }
                SettingsRow(icon = "🔍", label = "Page Zoom", value = "${settings.textZoom}%", showDivider = false) { showZoomDialog = true }
            }
        }

        item {
            SettingsSection(title = "ABOUT") {
                SettingsRow(icon = "ℹ️", label = "Version", value = "1.0", showDivider = false, onClick = {})
            }
        }

        item { HomeIndicator() }
    }

    if (showSearchEngineDialog) {
        SelectorDialog(
            title = "Search Engine",
            options = listOf("Google", "Bing", "DuckDuckGo"),
            selected = settings.searchEngine,
            onSelect = { viewModel.setSearchEngine(it); showSearchEngineDialog = false },
            onDismiss = { showSearchEngineDialog = false }
        )
    }

    if (showHomepageDialog) {
        HomepageDialog(
            current = settings.homepageUrl,
            onConfirm = { viewModel.setHomepageUrl(it); showHomepageDialog = false },
            onDismiss = { showHomepageDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectorDialog(
            title = "Theme",
            options = DarkModeOption.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            selected = settings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() },
            onSelect = { label ->
                viewModel.setDarkMode(DarkModeOption.valueOf(label.uppercase()))
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showZoomDialog) {
        ZoomDialog(
            current = settings.textZoom,
            onConfirm = { viewModel.setTextZoom(it); showZoomDialog = false },
            onDismiss = { showZoomDialog = false }
        )
    }

    if (showClearDataDialog) {
        ClearBrowsingDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = { clearHistory, clearCookies, clearCache, range ->
                historyViewModel.clearBrowsingData(clearHistory, clearCookies, clearCache, range)
                showClearDataDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = Orion.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) {
        Text(
            text = title,
            color = colors.textDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    value: String,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    val colors = Orion.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp)
            Box(modifier = Modifier.size(12.dp))
            Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) {
                Text(value, color = colors.textMid, fontSize = 13.sp)
                Box(modifier = Modifier.size(6.dp))
            }
            Text("›", color = colors.textDim, fontSize = 16.sp)
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp)
                    .height(1.dp)
                    .background(colors.border)
            )
        }
    }
}

@Composable
private fun ToggleRow(icon: String, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = Orion.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Box(modifier = Modifier.size(12.dp))
        Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, uncheckedTrackColor = colors.border)
        )
    }
}

@Composable
private fun SelectorDialog(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = Orion.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(title, color = colors.text) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(option, color = colors.text, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = colors.accent) }
        }
    )
}

@Composable
private fun HomepageDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = Orion.colors
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Homepage", color = colors.text) },
        text = {
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 14.sp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Leave blank for New Tab Page", color = colors.textDim, fontSize = 13.sp)
                    inner()
                }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) } }
    )
}

@Composable
private fun ZoomDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = Orion.colors
    var value by remember { mutableStateOf(current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Text Zoom", color = colors.text) },
        text = {
            Column {
                Text("${value.toInt()}%", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 50f..200f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.toInt()) }) { Text("Apply", color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) } }
    )
}
