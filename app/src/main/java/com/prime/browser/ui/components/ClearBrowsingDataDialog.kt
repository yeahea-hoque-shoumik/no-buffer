package com.prime.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.browser.history.TimeRange
import com.prime.browser.ui.theme.Orion

@Composable
fun ClearBrowsingDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (clearHistory: Boolean, clearCookies: Boolean, clearCache: Boolean, range: TimeRange) -> Unit
) {
    val colors = Orion.colors
    var clearHistory by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(false) }
    var clearCache by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(TimeRange.ALL_TIME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Clear Browsing Data", color = colors.text) },
        text = {
            Column {
                DataCheckboxRow("History", clearHistory) { clearHistory = it }
                DataCheckboxRow("Cookies", clearCookies) { clearCookies = it }
                DataCheckboxRow("Cache", clearCache) { clearCache = it }

                Text(
                    text = "TIME RANGE",
                    color = colors.textDim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
                TimeRange.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { range = option }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = range == option,
                            onClick = { range = option }
                        )
                        Text(option.label, color = colors.text, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clearHistory, clearCookies, clearCache, range) }) {
                Text("Clear", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMid)
            }
        }
    )
}

@Composable
private fun DataCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = Orion.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = colors.accent)
        )
        Text(label, color = colors.text, fontSize = 14.sp)
    }
}
