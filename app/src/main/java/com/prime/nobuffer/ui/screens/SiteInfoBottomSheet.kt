package com.prime.nobuffer.ui.screens

import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.browser.SiteCookies
import com.prime.nobuffer.ui.theme.Orion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInfoBottomSheet(
    url: String,
    javaScriptEnabled: Boolean,
    onDismiss: () -> Unit,
    onViewCookies: () -> Unit
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var cookieEpoch by remember { mutableIntStateOf(0) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val host = remember(url) { runCatching { Uri.parse(url).host }.getOrNull().orEmpty() }
    val isHttps = url.startsWith("https://", ignoreCase = true)
    val cookieCount = remember(url, cookieEpoch) { cookieCountFor(url) }

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
                Text(if (isHttps) "🔒" else "⚠️", fontSize = 20.sp)
                Box(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Site info", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = host.ifBlank { "Unknown host" },
                        color = colors.textMid,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            InfoRow("URL", url.ifBlank { "—" })
            InfoRow("Connection", if (isHttps) "HTTPS" else "Not secure")
            InfoRow("JavaScript", if (javaScriptEnabled) "Enabled" else "Disabled")
            InfoRow("Cookies", "$cookieCount")

            Box(modifier = Modifier.size(8.dp))

            SheetButton("View cookies") {
                onViewCookies()
            }
            SheetButton("Clear cookies for site") {
                expireCookiesForUrl(url)
                cookieEpoch++
                Toast.makeText(context, "Cleared cookies for this site", Toast.LENGTH_SHORT).show()
            }
            SheetButton("Clear all cookies") {
                confirmClearAll = true
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            containerColor = colors.surface,
            title = { Text("Clear all cookies?", color = colors.text) },
            text = {
                Text(
                    "This removes cookies for every site, not just ${host.ifBlank { "this page" }}.",
                    color = colors.textMid,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        cookieEpoch++
                        confirmClearAll = false
                        Toast.makeText(context, "All cookies cleared", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Clear all", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text("Cancel", color = colors.textMid)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = Orion.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(label, color = colors.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.size(4.dp))
        Text(value, color = colors.text, fontSize = 14.sp)
    }
}

@Composable
private fun SheetButton(label: String, onClick: () -> Unit) {
    val colors = Orion.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

internal fun cookieCountFor(url: String): Int {
    if (url.isBlank()) return 0
    val raw = CookieManager.getInstance().getCookie(url) ?: return 0
    return raw.split(';').map { it.trim() }.count { it.isNotEmpty() }
}

internal fun cookieEntriesFor(url: String): List<Pair<String, String>> {
    if (url.isBlank()) return emptyList()
    val raw = CookieManager.getInstance().getCookie(url) ?: return emptyList()
    return raw.split(';').map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
        val name = part.substringBefore('=').trim()
        val value = part.substringAfter('=', missingDelimiterValue = "").trim()
        name to value
    }
}

internal fun expireCookiesForUrl(url: String) {
    SiteCookies.expireHost(url)
}
