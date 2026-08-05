package com.prime.browser.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp
) {
    val textMeasurer = rememberTextMeasurer()
    val label = if (count > 99) "99+" else count.toString()
    val textStyle = TextStyle(
        color = color,
        fontSize = if (count > 9) 10.sp else 12.sp,
        fontWeight = FontWeight.Bold
    )
    val textLayout = textMeasurer.measure(label, textStyle)

    Canvas(modifier = modifier.size(size)) {
        drawRoundRect(
            color = color,
            size = Size(this.size.width, this.size.height),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                x = (this.size.width - textLayout.size.width) / 2f,
                y = (this.size.height - textLayout.size.height) / 2f
            )
        )
    }
}
