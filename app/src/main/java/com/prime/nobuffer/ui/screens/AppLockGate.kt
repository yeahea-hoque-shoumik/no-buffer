package com.prime.nobuffer.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.blocklist.AppLockController
import com.prime.nobuffer.ui.theme.Orion
import kotlinx.coroutines.launch

/**
 * Full-screen app lock. Other agents should show this when [AppLockController.shouldLock]
 * is true (typically on Activity create). Do not wrap [com.prime.nobuffer.MainActivity] here.
 */
@Composable
fun AppLockGate(
    controller: AppLockController,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val canBiometric = remember(controller, context) { controller.canUseBiometric(context) }

    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var biometricAttempted by remember { mutableStateOf(false) }

    fun succeed() {
        scope.launch {
            controller.markUnlocked()
            onUnlocked()
        }
    }

    fun tryPassword() {
        if (password.isEmpty() || busy) return
        scope.launch {
            busy = true
            error = null
            val fail = controller.unlockWithPassword(password)
            busy = false
            if (fail == null) onUnlocked() else error = fail
        }
    }

    fun tryBiometric() {
        val act = activity ?: return
        error = null
        controller.authenticateBiometric(
            activity = act,
            onSuccess = { succeed() },
            onUsePassword = { /* stay on password field */ },
            onError = { message -> error = message }
        )
    }

    LaunchedEffect(canBiometric, activity) {
        if (canBiometric && activity != null && !biometricAttempted) {
            biometricAttempted = true
            tryBiometric()
        }
    }

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
            Text(
                "NoBuffer is locked",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(8.dp))
            Text(
                "Enter the lock password to open the browser.",
                color = colors.textDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.size(22.dp))
            BasicTextField(
                value = password,
                onValueChange = { password = it; error = null },
                textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (password.isEmpty()) Text("Lock password", color = colors.textDim, fontSize = 14.sp)
                    inner()
                }
            )
            if (!error.isNullOrBlank()) {
                Text(
                    error!!,
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            Box(modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = password.isNotEmpty() && !busy
                    ) { tryPassword() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (busy) "Checking…" else "Unlock",
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (canBiometric) {
                Box(modifier = Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { tryBiometric() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Use biometric", color = colors.textMid, fontSize = 15.sp)
                }
            }
        }
    }
}

/** Convenience overlay that hides itself after a successful unlock. */
@Composable
fun AppLockOverlay(
    controller: AppLockController,
    modifier: Modifier = Modifier
) {
    var locked by remember { mutableStateOf(controller.shouldLock()) }
    if (!locked) return
    AppLockGate(
        controller = controller,
        onUnlocked = { locked = false },
        modifier = modifier
    )
}
