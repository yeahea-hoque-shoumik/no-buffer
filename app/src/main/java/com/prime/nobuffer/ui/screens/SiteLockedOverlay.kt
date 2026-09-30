package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.blocklist.BlockedHostMatcher
import com.prime.nobuffer.blocklist.SiteBlocker
import com.prime.nobuffer.blocklist.SiteLockPolicy
import com.prime.nobuffer.ui.components.VerifyLockPasswordDialog
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

private sealed class OverlayUnlockAction {
    data object Permanent : OverlayUnlockAction()
    data class Timed(val durationMillis: Long) : OverlayUnlockAction()
}

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
    val sites by app.siteBlocker.sites.collectAsStateWithLifecycle()
    val site = remember(sites, url) { app.siteBlocker.matchingSite(url) }
    val isGambling = remember(url) { app.siteBlocker.isGamblingUrl(url) }
    if (isGambling) {
        GamblingBlockedScreen(host = host, onGoHome = onGoHome, modifier = modifier)
        return
    }
    val now = System.currentTimeMillis()
    val remainingMillis = site?.let { SiteLockPolicy.remainingBudgetMillis(it, now) }
    val budgetExhausted = site != null && SiteLockPolicy.isBudgetExhausted(site, now)

    var pending by remember { mutableStateOf<OverlayUnlockAction?>(null) }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 36.sp)
            Box(modifier = Modifier.size(14.dp))
            Text(
                "This site is locked",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
            if (remainingMillis != null) {
                Box(modifier = Modifier.size(10.dp))
                Text(
                    if (budgetExhausted) {
                        "Daily budget used"
                    } else {
                        budgetRemainingLabel(remainingMillis)
                    },
                    color = if (budgetExhausted) colors.accent else colors.teal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            Box(modifier = Modifier.size(22.dp))
            if (!budgetExhausted) {
                Text(
                    "Unlock for",
                    color = colors.textMid,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SiteBlocker.TIMED_UNLOCK_MINUTES.forEach { minutes ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.elevated)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    error = null
                                    pending = OverlayUnlockAction.Timed(minutes * 60_000L)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${minutes}m", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Box(modifier = Modifier.size(10.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        error = null
                        pending = OverlayUnlockAction.Permanent
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Remove this site", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

    val action = pending
    if (action != null) {
        val title = when (action) {
            OverlayUnlockAction.Permanent -> "Unlock $host"
            is OverlayUnlockAction.Timed -> "Unlock $host for ${action.durationMillis / 60_000L}m"
        }
        VerifyLockPasswordDialog(
            title = title,
            error = error,
            busy = busy,
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = when (action) {
                        OverlayUnlockAction.Permanent -> app.siteBlocker.unlockSite(host, password)
                        is OverlayUnlockAction.Timed ->
                            app.siteBlocker.timedUnlock(host, password, action.durationMillis)
                    }
                    busy = false
                    if (fail == null) {
                        pending = null
                        onUnlocked()
                    } else {
                        error = fail
                    }
                }
            },
            onDismiss = { pending = null }
        )
    }
}

private fun budgetRemainingLabel(remainingMillis: Long): String {
    val minutes = remainingMillis / 60_000L
    return when {
        remainingMillis <= 0L -> "0 min left today"
        minutes <= 0L -> "Less than 1 min left today"
        minutes == 1L -> "1 min left today"
        else -> "$minutes min left today"
    }
}

/** Forced block page: no password prompt, no unlock button — gambling sites cannot be unblocked. */
@Composable
private fun GamblingBlockedScreen(host: String, onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Orion.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚫", fontSize = 36.sp)
            Box(modifier = Modifier.size(14.dp))
            Text(
                "Gambling sites are blocked",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(8.dp))
            Text(host, color = colors.textMid, fontSize = 14.sp, textAlign = TextAlign.Center)
            Box(modifier = Modifier.size(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGoHome)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Go to New Tab", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
