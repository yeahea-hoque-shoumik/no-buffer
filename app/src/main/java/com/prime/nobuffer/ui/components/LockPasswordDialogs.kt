package com.prime.nobuffer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.blocklist.LockPassword
import com.prime.nobuffer.ui.theme.Orion

@Composable
fun SetLockPasswordDialog(
    title: String = "Set lock password",
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Orion.colors
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val policy = LockPassword.evaluate(password)
    val mismatch = confirm.isNotEmpty() && confirm != password
    val canSave = policy.isValid && password == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(title, color = colors.text) },
        text = {
            Column {
                Text(
                    "20+ characters, mixed upper/lower, a number, and a symbol. This is the only way to unblock a site.",
                    color = colors.textMid,
                    fontSize = 12.sp
                )
                BoxSpacer()
                PasswordField(value = password, onValueChange = { password = it }, hint = "New password")
                BoxSpacer()
                PasswordField(value = confirm, onValueChange = { confirm = it }, hint = "Confirm password")
                BoxSpacer()
                PolicyChecklist(policy)
                if (mismatch) {
                    Text("Passwords don’t match", color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canSave) onConfirm(password) }, enabled = canSave) {
                Text("Save", color = if (canSave) colors.accent else colors.textDim)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) } }
    )
}

@Composable
fun VerifyLockPasswordDialog(
    title: String,
    error: String?,
    busy: Boolean = false,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Orion.colors
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(title, color = colors.text) },
        text = {
            Column {
                Text("Enter the lock password to continue.", color = colors.textMid, fontSize = 12.sp)
                BoxSpacer()
                PasswordField(value = password, onValueChange = { password = it }, hint = "Lock password")
                if (!error.isNullOrBlank()) {
                    Text(error, color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotEmpty() && !busy) onConfirm(password) },
                enabled = password.isNotEmpty() && !busy
            ) {
                Text(if (busy) "Checking…" else "Unlock", color = if (password.isNotEmpty() && !busy) colors.accent else colors.textDim)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) } }
    )
}

@Composable
fun ChangeLockPasswordDialog(
    error: String?,
    onConfirm: (current: String, next: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = Orion.colors
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val policy = LockPassword.evaluate(next)
    val canSave = current.isNotEmpty() && policy.isValid && next == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Change lock password", color = colors.text) },
        text = {
            Column {
                PasswordField(value = current, onValueChange = { current = it }, hint = "Current password")
                BoxSpacer()
                PasswordField(value = next, onValueChange = { next = it }, hint = "New password")
                BoxSpacer()
                PasswordField(value = confirm, onValueChange = { confirm = it }, hint = "Confirm new password")
                BoxSpacer()
                PolicyChecklist(policy)
                if (confirm.isNotEmpty() && confirm != next) {
                    Text("Passwords don’t match", color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (!error.isNullOrBlank()) {
                    Text(error, color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canSave) onConfirm(current, next) }, enabled = canSave) {
                Text("Save", color = if (canSave) colors.accent else colors.textDim)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMid) } }
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, hint: String) {
    val colors = Orion.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.elevated)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(hint, color = colors.textDim, fontSize = 14.sp)
            inner()
        }
    )
}

@Composable
private fun PolicyChecklist(policy: LockPassword.PolicyCheck) {
    val colors = Orion.colors
    Column {
        PolicyLine("20+ characters", policy.minLength && policy.maxLength)
        PolicyLine("Uppercase + lowercase", policy.upper && policy.lower)
        PolicyLine("A number", policy.digit)
        PolicyLine("A symbol", policy.special)
        PolicyLine("No spaces", policy.noWhitespace)
    }
}

@Composable
private fun PolicyLine(label: String, ok: Boolean) {
    val colors = Orion.colors
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(if (ok) "✓" else "○", color = if (ok) colors.teal else colors.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(8.dp))
        Text(label, color = if (ok) colors.text else colors.textMid, fontSize = 12.sp)
    }
}

@Composable
private fun BoxSpacer() {
    androidx.compose.foundation.layout.Box(modifier = Modifier.size(10.dp))
}
