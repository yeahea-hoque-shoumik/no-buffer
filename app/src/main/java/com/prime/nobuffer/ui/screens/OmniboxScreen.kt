package com.prime.nobuffer.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.prime.nobuffer.omnibox.OmniboxSuggestion
import com.prime.nobuffer.omnibox.OmniboxViewModel
import com.prime.nobuffer.omnibox.SuggestionType
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion

@Composable
fun OmniboxScreen(
    initialUrl: String,
    onNavigate: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OmniboxViewModel = viewModel()
) {
    val colors = Orion.colors
    val context = LocalContext.current
    val prefill = if (initialUrl == "about:blank") "" else initialUrl

    val fieldState = remember { TextFieldState(prefill, TextRange(0, prefill.length)) }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }
            .collect { viewModel.onQueryChange(it) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submit(text: String) {
        if (text.isNotBlank()) onNavigate(viewModel.resolveInput(text))
    }

    fun applyRecognizedText(text: String, autoSubmit: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        fieldState.edit { replace(0, length, trimmed) }
        if (autoSubmit) submit(trimmed)
    }

    val zxingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contents = result.data?.getStringExtra("SCAN_RESULT")
        if (!contents.isNullOrBlank()) applyRecognizedText(contents, autoSubmit = true)
        else if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "No QR code found", Toast.LENGTH_SHORT).show()
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        decodeQrBitmap(bitmap) { value ->
            if (!value.isNullOrBlank()) applyRecognizedText(value, autoSubmit = true)
            else Toast.makeText(context, "No QR code found", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraQr() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(context, "No camera available — paste a QR URL instead", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            captureLauncher.launch(null)
        } catch (_: Exception) {
            Toast.makeText(context, "Couldn't open camera — paste a QR URL instead", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraQr()
        else Toast.makeText(context, "Camera permission is needed to scan QR codes", Toast.LENGTH_SHORT).show()
    }

    fun startQrScan() {
        val zxing = Intent("com.google.zxing.client.android.SCAN").apply {
            putExtra("SCAN_MODE", "QR_CODE_MODE")
        }
        if (zxing.resolveActivity(context.packageManager) != null) {
            try {
                zxingLauncher.launch(zxing)
                return
            } catch (_: Exception) {
                // Fall through to in-app camera + ML Kit.
            }
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCameraQr() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.surface)
                    .border(1.5.dp, colors.accent, RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = colors.accent, fontSize = 16.sp)
                Box(modifier = Modifier.size(8.dp))
                BasicTextField(
                    state = fieldState,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = colors.text, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    onKeyboardAction = KeyboardActionHandler { submit(fieldState.text.toString()) },
                    lineLimits = TextFieldLineLimits.SingleLine
                )
                Box(modifier = Modifier.size(8.dp))
                OmniboxIconButton("QR", colors.accent) { startQrScan() }
            }

            Box(modifier = Modifier.size(12.dp))

            Text(
                text = "Cancel",
                color = colors.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                )
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(suggestions) { suggestion ->
                SuggestionRow(suggestion = suggestion, onClick = { submit(suggestion.text) })
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: OmniboxSuggestion, onClick: () -> Unit) {
    val colors = Orion.colors
    val tagColor = when (suggestion.type) {
        SuggestionType.RECENT -> colors.accent
        SuggestionType.BOOKMARK -> colors.teal
        SuggestionType.SEARCH -> colors.textDim
    }
    val glyph = when (suggestion.type) {
        SuggestionType.RECENT -> "🕐"
        SuggestionType.BOOKMARK -> "★"
        SuggestionType.SEARCH -> "⌕"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 14.sp)
        }

        Box(modifier = Modifier.size(12.dp))

        Text(
            text = suggestion.text,
            color = colors.text,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(modifier = Modifier.size(8.dp))

        Text(
            text = suggestion.type.name,
            color = tagColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OmniboxIconButton(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}

private fun decodeQrBitmap(bitmap: Bitmap, onResult: (String?) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    BarcodeScanning.getClient()
        .process(image)
        .addOnSuccessListener { barcodes ->
            val value = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                ?: barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
            onResult(value)
        }
        .addOnFailureListener { onResult(null) }
}


