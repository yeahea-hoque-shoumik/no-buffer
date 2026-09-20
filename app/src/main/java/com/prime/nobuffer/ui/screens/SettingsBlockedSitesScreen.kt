package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.blocklist.AppLockStore
import com.prime.nobuffer.blocklist.SiteBlocker
import com.prime.nobuffer.blocklist.SiteLockPolicy
import com.prime.nobuffer.data.entity.BlockedSite
import com.prime.nobuffer.ui.components.ChangeLockPasswordDialog
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.SetLockPasswordDialog
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.components.VerifyLockPasswordDialog
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val BUDGET_OPTIONS = intArrayOf(0, 15, 30, 60, 120)
private val UNLOCK_UNTIL_FORMAT = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun SettingsBlockedSitesScreen(
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val scope = rememberCoroutineScope()
    val appLockStore = remember { AppLockStore(app) }

    val sites by app.siteBlocker.sites.collectAsStateWithLifecycle()
    val hasPassword by app.siteBlocker.hasPassword.collectAsStateWithLifecycle()
    val appLockEnabled by appLockStore.enabled.collectAsStateWithLifecycle(initialValue = false)

    var draft by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var showSetPassword by remember { mutableStateOf(false) }
    var enableAppLockAfterSetPassword by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var changeError by remember { mutableStateOf<String?>(null) }
    var pendingEditHost by remember { mutableStateOf<String?>(null) }
    var editingHost by remember { mutableStateOf<String?>(null) }
    var sheetPassword by remember { mutableStateOf<String?>(null) }
    var pendingClearAll by remember { mutableStateOf(false) }
    var pendingEnableAppLock by remember { mutableStateOf(false) }
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock NoBuffer on open", color = colors.text, fontSize = 15.sp)
                            Text(
                                "Requires the lock password (or biometric) after launch.",
                                color = colors.textDim,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    scope.launch { appLockStore.setEnabled(false) }
                                } else if (!hasPassword) {
                                    enableAppLockAfterSetPassword = true
                                    showSetPassword = true
                                } else {
                                    verifyError = null
                                    pendingEnableAppLock = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = colors.accent,
                                uncheckedTrackColor = colors.border
                            )
                        )
                    }
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
                    BlockedSiteRow(site = site) {
                        verifyError = null
                        pendingEditHost = site.host
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
                        if (enableAppLockAfterSetPassword) {
                            enableAppLockAfterSetPassword = false
                            appLockStore.setEnabled(true)
                        }
                        if (draft.isNotBlank()) {
                            val addFail = app.siteBlocker.addSite(draft)
                            if (addFail == null) draft = "" else addError = addFail
                        }
                    } else {
                        addError = fail
                    }
                }
            },
            onDismiss = {
                showSetPassword = false
                enableAppLockAfterSetPassword = false
            }
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

    val verifyHost = pendingEditHost
    if (verifyHost != null) {
        VerifyLockPasswordDialog(
            title = "Edit $verifyHost",
            error = verifyError,
            busy = busy,
            confirmLabel = "Continue",
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = verifyLockPassword(app, password)
                    busy = false
                    if (fail == null) {
                        sheetPassword = password
                        editingHost = verifyHost
                        pendingEditHost = null
                    } else {
                        verifyError = fail
                    }
                }
            },
            onDismiss = { pendingEditHost = null }
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

    if (pendingEnableAppLock) {
        VerifyLockPasswordDialog(
            title = "Enable app lock",
            error = verifyError,
            busy = busy,
            confirmLabel = "Enable",
            onConfirm = { password ->
                scope.launch {
                    busy = true
                    val fail = verifyLockPassword(app, password)
                    busy = false
                    if (fail == null) {
                        appLockStore.setEnabled(true)
                        pendingEnableAppLock = false
                    } else {
                        verifyError = fail
                    }
                }
            },
            onDismiss = { pendingEnableAppLock = false }
        )
    }

    val editHost = editingHost
    val editSite = editHost?.let { h -> sites.find { it.host == h } }
    val passwordForSheet = sheetPassword
    if (editHost != null && editSite != null && passwordForSheet != null) {
        SiteLockEditSheet(
            site = editSite,
            password = passwordForSheet,
            onDismiss = {
                editingHost = null
                sheetPassword = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedSiteRow(site: BlockedSite, onClick: () -> Unit) {
    val colors = Orion.colors
    val now = System.currentTimeMillis()
    val locked = SiteLockPolicy.isActivelyLocked(site, now)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(site.host, color = colors.text, fontSize = 15.sp)
            Box(modifier = Modifier.size(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (locked) {
                    StatusChip("Locked", accent = true)
                } else if (site.unlockUntil > now) {
                    StatusChip("Unlocked until ${formatUnlockUntil(site.unlockUntil)}")
                } else {
                    StatusChip("Unlocked")
                }
                if (site.dailyBudgetMinutes > 0) {
                    val remaining = SiteLockPolicy.remainingBudgetMillis(site, now)
                    val label = if (remaining == null) {
                        "${site.dailyBudgetMinutes}m/day"
                    } else {
                        "${remaining / 60_000L}m left · ${site.dailyBudgetMinutes}m/day"
                    }
                    StatusChip(label)
                }
                if (SiteLockPolicy.isScheduleSet(site)) {
                    StatusChip(
                        "${SiteLockPolicy.formatMinuteOfDay(site.scheduleStartMinute)}–" +
                            SiteLockPolicy.formatMinuteOfDay(site.scheduleEndMinute)
                    )
                }
                if (site.videoAllowed) {
                    StatusChip("Video allowed")
                }
            }
        }
        Text("›", color = colors.textDim, fontSize = 16.sp)
    }
}

@Composable
private fun StatusChip(label: String, accent: Boolean = false) {
    val colors = Orion.colors
    Text(
        text = label,
        color = if (accent) colors.accent else colors.textMid,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteLockEditSheet(
    site: BlockedSite,
    password: String,
    onDismiss: () -> Unit
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var startText by remember {
        mutableStateOf(
            if (SiteLockPolicy.isScheduleSet(site)) SiteLockPolicy.formatMinuteOfDay(site.scheduleStartMinute) else ""
        )
    }
    var endText by remember {
        mutableStateOf(
            if (SiteLockPolicy.isScheduleSet(site)) SiteLockPolicy.formatMinuteOfDay(site.scheduleEndMinute) else ""
        )
    }
    var daysMask by remember { mutableIntStateOf(site.scheduleDaysMask) }

    LaunchedEffect(site.host, site.scheduleStartMinute, site.scheduleEndMinute, site.scheduleDaysMask) {
        if (SiteLockPolicy.isScheduleSet(site)) {
            startText = SiteLockPolicy.formatMinuteOfDay(site.scheduleStartMinute)
            endText = SiteLockPolicy.formatMinuteOfDay(site.scheduleEndMinute)
        }
        daysMask = site.scheduleDaysMask
    }

    fun run(block: suspend () -> String?) {
        scope.launch {
            busy = true
            error = null
            val fail = block()
            busy = false
            if (fail != null) error = fail
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(site.host, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Changes require the lock password already verified for this sheet.",
                color = colors.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            if (!error.isNullOrBlank()) {
                Text(error!!, color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
            }

            Text("Timed unlock", color = colors.textMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Box(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SiteBlocker.TIMED_UNLOCK_MINUTES.forEach { minutes ->
                    ChoiceChip(
                        label = "${minutes}m",
                        selected = false,
                        modifier = Modifier.weight(1f),
                        enabled = !busy && !SiteLockPolicy.isBudgetExhausted(site, System.currentTimeMillis())
                    ) {
                        run { app.siteBlocker.timedUnlock(site.host, password, minutes * 60_000L) }
                    }
                }
            }
            Box(modifier = Modifier.size(10.dp))
            SheetButton("Quick lock", outline = true, enabled = !busy) {
                run { app.siteBlocker.quickLock(site.host) }
            }

            Box(modifier = Modifier.size(18.dp))
            Text("Schedule", color = colors.textMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                "Locks only during this window on selected days. Wraps past midnight (22:00–07:00).",
                color = colors.textDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TimeField(value = startText, hint = "22:00", modifier = Modifier.weight(1f)) { startText = it }
                TimeField(value = endText, hint = "07:00", modifier = Modifier.weight(1f)) { endText = it }
            }
            Box(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                DAY_LABELS.forEachIndexed { index, label ->
                    val selected = SiteLockPolicy.dayInMask(daysMask, index + 1)
                    ChoiceChip(
                        label = label,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        enabled = !busy
                    ) {
                        daysMask = daysMask xor (1 shl index)
                    }
                }
            }
            Box(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SheetButton("Save schedule", modifier = Modifier.weight(1f), enabled = !busy) {
                    val startBlank = startText.isBlank()
                    val endBlank = endText.isBlank()
                    if (startBlank && endBlank) {
                        run { app.siteBlocker.setSchedule(site.host, -1, -1, daysMask, password) }
                    } else {
                        val start = SiteLockPolicy.parseMinuteOfDay(startText)
                        val end = SiteLockPolicy.parseMinuteOfDay(endText)
                        if (start == null || end == null) {
                            error = "Enter times like 22:00 and 07:00"
                        } else {
                            run { app.siteBlocker.setSchedule(site.host, start, end, daysMask, password) }
                        }
                    }
                }
                SheetButton("Clear", outline = true, modifier = Modifier.weight(1f), enabled = !busy) {
                    startText = ""
                    endText = ""
                    run { app.siteBlocker.setSchedule(site.host, -1, -1, daysMask, password) }
                }
            }

            Box(modifier = Modifier.size(18.dp))
            Text("Daily budget", color = colors.textMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Box(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BUDGET_OPTIONS.forEach { minutes ->
                    ChoiceChip(
                        label = if (minutes == 0) "Off" else "${minutes}m",
                        selected = site.dailyBudgetMinutes == minutes,
                        modifier = Modifier.weight(1f),
                        enabled = !busy
                    ) {
                        run { app.siteBlocker.setDailyBudget(site.host, minutes, password) }
                    }
                }
            }

            Box(modifier = Modifier.size(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow video on this site", color = colors.text, fontSize = 15.sp)
                    Text(
                        "Exception to the global video block while the site is navigable.",
                        color = colors.textDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = site.videoAllowed,
                    onCheckedChange = { allowed ->
                        run { app.siteBlocker.setVideoAllowed(site.host, allowed, password) }
                    },
                    enabled = !busy,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        uncheckedTrackColor = colors.border
                    )
                )
            }

            Box(modifier = Modifier.size(18.dp))
            SheetButton("Remove permanently", outline = true, enabled = !busy) {
                scope.launch {
                    busy = true
                    error = null
                    val fail = app.siteBlocker.unlockSite(site.host, password)
                    busy = false
                    if (fail == null) onDismiss() else error = fail
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = Orion.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.accent else colors.elevated)
            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) colors.text else colors.textDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimeField(
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val colors = Orion.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
        singleLine = true,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(hint, color = colors.textDim, fontSize = 14.sp)
            inner()
        }
    )
}

@Composable
private fun SheetButton(
    label: String,
    modifier: Modifier = Modifier,
    outline: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = Orion.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (outline) colors.elevated else colors.accent)
            .then(
                if (outline) Modifier.border(1.dp, colors.border, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) colors.text else colors.textDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

private fun formatUnlockUntil(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(UNLOCK_UNTIL_FORMAT)
}

private suspend fun verifyLockPassword(app: BrowserApplication, password: String): String? =
    withContext(Dispatchers.Default) {
        when (val result = app.siteBlocker.passwordStore.verify(password)) {
            com.prime.nobuffer.blocklist.PasswordVerifyResult.Ok -> null
            com.prime.nobuffer.blocklist.PasswordVerifyResult.NoPassword -> "No lock password is set"
            is com.prime.nobuffer.blocklist.PasswordVerifyResult.Wrong ->
                SiteBlocker.wrongPasswordMessage(result)
            is com.prime.nobuffer.blocklist.PasswordVerifyResult.Locked ->
                com.prime.nobuffer.blocklist.LockPasswordStore.lockoutMessage(result.remainingMillis)
        }
    }
