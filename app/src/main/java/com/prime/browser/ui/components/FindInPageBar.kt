package com.prime.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.browser.ui.theme.Orion

@Composable
fun FindInPageBar(
    query: String,
    activeMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 14.sp)
        )
        if (query.isNotEmpty()) {
            Text(
                text = "${if (totalMatches > 0) activeMatch + 1 else 0}/$totalMatches",
                color = colors.textDim,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onPrevious) { Text("‹", color = colors.textMid, fontSize = 18.sp) }
        IconButton(onClick = onNext) { Text("›", color = colors.textMid, fontSize = 18.sp) }
        IconButton(onClick = onClose) { Text("✕", color = colors.textMid, fontSize = 16.sp) }
    }
}
