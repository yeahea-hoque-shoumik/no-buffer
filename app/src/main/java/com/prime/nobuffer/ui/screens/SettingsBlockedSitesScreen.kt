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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.ui.components.ChangeLockPasswordDialog
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.SetLockPasswordDialog
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.components.VerifyLockPasswordDialog
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

@Composable
fun SettingsBlockedSitesScreen(
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val scope = rememberCoroutineScope()

    val sites by app.siteBlocker.sites.collectAsStateWithLifecycle()
    val hasPassword by app.siteBlocker.hasPassword.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var showSetPassword by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var changeError by remember { mutableStateOf<String?>(null) }
    var pendingRemoveHost by remember { mutableStateOf<String?>(null) }
    var pendingClearAll by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()
        Text(
            text = "Blocked Sites",
            color = colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp)
        )
        Text(
            text = "Adding a site is instant. Removing one requires the 20-character lock password. This only applies inside NoBuffer.",
            color = colors.textMid,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text("Add a site", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.size(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it; addError = null },
                            textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.elevated)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            decorationBox = { inner ->
                                if (draft.isEmpty()) Text("youtube.com", color = colors.textDim, fontSize = 14.sp)
                                inner()
                            }
                        )
                        Box(modifier = Modifier.size(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (!hasPassword) {
                                        showSetPassword = true
                                        return@clickable
                                    }
                                    scope.launch {
                                        val fail = app.siteBlocker.addSite(draft)
                                        if (fail == null) draft = "" else addError = fail
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("Block", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (!addError.isNullOrBlank()) {
                        Text(addError!!, color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        "Subdomains are included (m.youtube.com, www.youtube.com).",
                        color = colors.textDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item { Box(modifier = Modifier.size(16.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                ) {
                    ActionRow(
                        label = if (hasPassword) "Change lock password" else "Set lock password",
                        showDivider = hasPassword && sites.isNotEmpty()
                    ) {
                        if (hasPassword) {
                            changeError = null
                            showChangePassword = true
                        } else {
                            showSetPassword = true
                        }
                    }
                    if (hasPassword && sites.isNotEmpty()) {
                        ActionRow(label = "Remove all blocked sites", showDivider = false) {
                            verifyError = null
                            pendingClearAll = true
                        }
                    }
                }
            }

            item { Box(modifier = Modifier.size(16.dp)) }

            if (sites.isEmpty()) {
                item {
                    Text(
                        "No sites blocked yet.",
                        color = colors.textDim,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(sites, key = { it.host }) { site ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(site.host, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(
                            "Unlock",
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                verifyError = null
                                pendingRemoveHost = site.host
                            }
                        )
                    }
                }
            }

            item { Box(modifier = Modifier.height(20.dp)) }
        }

        HomeIndicator()
    }

    if (showSetPassword) {
        SetLockPasswordDialog(
            onConfirm = { password ->
                scope.launch {
                    val fail = app.siteBlocker.passwordStore.setPassword(password)
                    if (fail == null) {
                        showSetPassword = false
                        if (draft.isNotBlank()) {
                            val addFail = app.siteBlocker.addSite(draft)
                            if (addFail == null) draft = "" else addError = addFail
                        }
                    } else {
                        addError = fail
                    }
                }
            },
            onDismiss = { showSetPassword = false }
        )
    }

    if (showChangePassword) {
        ChangeLockPasswordDialog(
            error = changeError,
            onConfirm = { current, next ->
                scope.launch {
                    val fail = app.siteBlocker.passwordStore.changePassword(current, next)
                    if (fail == null) showChangePassword = false else changeError = fail
                }
            },
            onDismiss = { showChangePassword = false }
        )
    }

    val verifyHost = pendingRemoveHost
    if (verifyHost != null) {
        VerifyLockPasswordDialog(
            title = "Unlock $verifyHost",
            error = verifyError,
            busy = busy,
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = app.siteBlocker.unlockSite(verifyHost, password)
                    busy = false
                    if (fail == null) pendingRemoveHost = null else verifyError = fail
                }
            },
            onDismiss = { pendingRemoveHost = null }
        )
    }

    if (pendingClearAll) {
        VerifyLockPasswordDialog(
            title = "Remove all blocked sites",
            error = verifyError,
            busy = busy,
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = app.siteBlocker.unlockAll(password)
                    busy = false
                    if (fail == null) pendingClearAll = false else verifyError = fail
                }
            },
            onDismiss = { pendingClearAll = false }
        )
    }
}

@Composable
private fun ActionRow(label: String, showDivider: Boolean, onClick: () -> Unit) {
    val colors = Orion.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = colors.textDim, fontSize = 16.sp)
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .height(1.dp)
                    .background(colors.border)
            )
        }
    }
}
