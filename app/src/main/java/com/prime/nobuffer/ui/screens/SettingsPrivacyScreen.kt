package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.settings.SettingsViewModel
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

@Composable
fun SettingsPrivacyScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val colors = Orion.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val coroutineScope = rememberCoroutineScope()

    var customRuleCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { customRuleCount = app.cosmeticRuleStore.customFilterRuleCount() }

    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()

        Text(
            text = "Privacy & Security",
            color = colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) {
            PrivacyToggle("Cookies", checked = true, enabled = false) { }
            PrivacyToggle("Block 3rd-party Cookies", settings.blockThirdPartyCookies) { viewModel.setBlockThirdPartyCookies(it) }
            PrivacyToggle("Do Not Track", settings.doNotTrackEnabled) { viewModel.setDoNotTrackEnabled(it) }
            PrivacyToggle("Safe Browsing", settings.safeBrowsingEnabled) { viewModel.setSafeBrowsingEnabled(it) }
            PrivacyToggle("Anti-Fingerprinting", settings.antiFingerprintingEnabled) { viewModel.setAntiFingerprintingEnabled(it) }
            PrivacyToggle("Search Suggestions", settings.searchSuggestionsEnabled, showDivider = false) { viewModel.setSearchSuggestionsEnabled(it) }
        }

        Box(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showImportDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Filter List", color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    text = if (customRuleCount > 0) "$customRuleCount rules" else "None imported",
                    color = colors.textMid,
                    fontSize = 13.sp
                )
                Box(modifier = Modifier.size(6.dp))
                Text("›", color = colors.textDim, fontSize = 16.sp)
            }
        }

        Box(modifier = Modifier.weight(1f))
        HomeIndicator()
    }

    if (showImportDialog) {
        CustomFilterListDialog(
            currentCount = customRuleCount,
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                coroutineScope.launch {
                    app.cosmeticRuleStore.importCustomFilterList(text)
                    customRuleCount = app.cosmeticRuleStore.customFilterRuleCount()
                }
                showImportDialog = false
            },
            onClear = {
                coroutineScope.launch {
                    app.cosmeticRuleStore.clearCustomFilterList()
                    customRuleCount = 0
                }
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun CustomFilterListDialog(
    currentCount: Int,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onClear: () -> Unit
) {
    val colors = Orion.colors
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Custom Filter List", color = colors.text) },
        text = {
            Column {
                Text(
                    "Paste a basic Adblock Plus-style list — plain domain rules (||domain.com^) and " +
                        "simple cosmetic hides (domain.com##.selector). $currentCount rules currently imported.",
                    color = colors.textMid,
                    fontSize = 12.sp
                )
                Box(modifier = Modifier.size(10.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = colors.text, fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.elevated)
                        .padding(10.dp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text("||ads.example.com^\nexample.com##.ad-banner", color = colors.textDim, fontSize = 12.sp)
                        inner()
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onImport(text) }) { Text("Import", color = colors.accent) } },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear All", color = colors.textMid) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) }
            }
        }
    )
}

@Composable
private fun PrivacyToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = Orion.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, uncheckedTrackColor = colors.border)
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
