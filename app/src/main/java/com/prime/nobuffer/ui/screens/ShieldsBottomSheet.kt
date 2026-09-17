package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.prime.nobuffer.shields.EffectiveShields
import com.prime.nobuffer.ui.theme.Orion

/** Phase 20 — per-site shield override sheet, opened from the [com.prime.nobuffer.ui.components.PillBar] shield icon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldsBottomSheet(
    host: String,
    effective: EffectiveShields,
    hasOverride: Boolean,
    onDismiss: () -> Unit,
    onSetAdBlock: (Boolean) -> Unit,
    onSetTrackerBlock: (Boolean) -> Unit,
    onSetScriptsEnabled: (Boolean) -> Unit,
    onSetFingerprintProtection: (Boolean) -> Unit,
    onResetToDefault: () -> Unit
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
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 20.sp)
                Box(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shields", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(host, color = colors.textMid, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (hasOverride) {
                    Text(
                        text = "Reset",
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onResetToDefault
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                ShieldToggle("Ad Blocking", effective.adBlockEnabled, onSetAdBlock)
                ShieldToggle("Tracker Blocking", effective.trackerBlockEnabled, onSetTrackerBlock)
                ShieldToggle("Scripts (JavaScript)", effective.scriptsEnabled, onSetScriptsEnabled)
                ShieldToggle("Fingerprint Protection", effective.fingerprintProtectionEnabled, onSetFingerprintProtection, showDivider = false)
            }
        }
    }
}

@Composable
private fun ShieldToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, showDivider: Boolean = true) {
    val colors = Orion.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, uncheckedTrackColor = colors.border)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .background(colors.border)
                    .size(width = 0.dp, height = 1.dp)
            )
        }
    }
}
