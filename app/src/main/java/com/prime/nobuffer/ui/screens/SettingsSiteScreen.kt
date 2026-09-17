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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.nobuffer.settings.SettingsViewModel
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion

@Composable
fun SettingsSiteScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val colors = Orion.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showTtlDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()

        Text(
            text = "Site Settings",
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
            SiteToggle("📍 Location", settings.locationPermission) { viewModel.setLocationPermission(it) }
            SiteToggle("🎙️ Microphone", settings.micPermission) { viewModel.setMicPermission(it) }
            SiteToggle("📷 Camera", settings.cameraPermission) { viewModel.setCameraPermission(it) }
            SiteToggle("🔔 Notifications", settings.notificationsPermission) { viewModel.setNotificationsPermission(it) }
            SiteToggle("🪟 Pop-ups", !settings.popupsBlocked) { viewModel.setPopupsBlocked(!it) }
            SiteToggle("⚙️ JavaScript", settings.javaScriptEnabled) { viewModel.setJavaScriptEnabled(it) }
            SiteToggle("🎬 Media (Video)", checked = false, enabled = false, showDivider = false) { }
        }

        Box(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showTtlDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱️ Permission Grant Duration", color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(ttlLabel(settings.permissionGrantTtlHours), color = colors.textMid, fontSize = 13.sp)
                Box(modifier = Modifier.size(6.dp))
                Text("›", color = colors.textDim, fontSize = 16.sp)
            }
        }

        Box(modifier = Modifier.weight(1f))
        HomeIndicator()
    }

    if (showTtlDialog) {
        val options = listOf(1 to "1 hour", 24 to "24 hours", 168 to "7 days", -1 to "Ask every time")
        AlertDialog(
            onDismissRequest = { showTtlDialog = false },
            containerColor = colors.surface,
            title = { Text("Permission Grant Duration", color = colors.text) },
            text = {
                Column {
                    options.forEach { (hours, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    viewModel.setPermissionGrantTtlHours(hours)
                                    showTtlDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = settings.permissionGrantTtlHours == hours, onClick = {
                                viewModel.setPermissionGrantTtlHours(hours)
                                showTtlDialog = false
                            })
                            Text(label, color = colors.text, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTtlDialog = false }) { Text("Close", color = colors.accent) } }
        )
    }
}

private fun ttlLabel(hours: Int): String = when (hours) {
    -1 -> "Ask every time"
    1 -> "1 hour"
    24 -> "24 hours"
    168 -> "7 days"
    else -> "${hours}h"
}

@Composable
private fun SiteToggle(
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
            Text(label, color = if (enabled) colors.text else colors.textDim, fontSize = 15.sp, modifier = Modifier.weight(1f))
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
