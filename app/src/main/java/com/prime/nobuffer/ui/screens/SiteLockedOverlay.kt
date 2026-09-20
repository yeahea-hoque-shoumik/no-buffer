package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.blocklist.BlockedHostMatcher
import com.prime.nobuffer.ui.components.VerifyLockPasswordDialog
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

@Composable
fun SiteLockedOverlay(
    url: String,
    onUnlocked: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val host = BlockedHostMatcher.hostFromUrl(url) ?: url
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val scope = rememberCoroutineScope()

    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 36.sp)
            Box(modifier = Modifier.size(14.dp))
            Text("This site is locked", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Box(modifier = Modifier.size(8.dp))
            Text(
                host,
                color = colors.textMid,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(6.dp))
            Text(
                "Unblocking requires the lock password.",
                color = colors.textDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        error = null
                        showPassword = true
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Unlock this site", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGoHome)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Go to New Tab", color = colors.textMid, fontSize = 15.sp)
            }
        }
    }

    if (showPassword) {
        VerifyLockPasswordDialog(
            title = "Unlock $host",
            error = error,
            busy = busy,
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = app.siteBlocker.unlockSite(host, password)
                    busy = false
                    if (fail == null) {
                        showPassword = false
                        onUnlocked()
                    } else {
                        error = fail
                    }
                }
            },
            onDismiss = { showPassword = false }
        )
    }
}
