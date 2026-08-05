package com.prime.browser.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prime.browser.omnibox.OmniboxSuggestion
import com.prime.browser.omnibox.OmniboxViewModel
import com.prime.browser.omnibox.SuggestionType
import com.prime.browser.ui.components.StatusBar
import com.prime.browser.ui.theme.Orion

@Composable
fun OmniboxScreen(
    initialUrl: String,
    onNavigate: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OmniboxViewModel = viewModel()
) {
    val colors = Orion.colors
    val prefill = if (initialUrl == "about:blank") "" else initialUrl

    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = prefill, selection = TextRange(0, prefill.length)))
    }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        viewModel.onQueryChange(prefill)
    }

    fun submit(text: String) {
        if (text.isNotBlank()) onNavigate(resolveInput(text))
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
                    value = fieldValue,
                    onValueChange = {
                        fieldValue = it
                        viewModel.onQueryChange(it.text)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(color = colors.text, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit(fieldValue.text) })
                )
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

private fun resolveInput(input: String): String {
    val trimmed = input.trim()
    val looksLikeUrl = trimmed.contains(".") && !trimmed.contains(" ")
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        looksLikeUrl -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}
