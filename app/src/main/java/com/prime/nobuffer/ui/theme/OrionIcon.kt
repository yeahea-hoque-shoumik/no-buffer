package com.prime.nobuffer.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun OrionIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Orion.colors.accent,
    glow: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        if (glow) {
            drawCircle(color = color.copy(alpha = 0.35f), radius = radius * 1.15f, center = center)
        }

        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = radius * 0.85f,
            center = center,
            style = Stroke(width = radius * 0.09f)
        )

        rotate(degrees = -32f, pivot = center) {
            drawOval(
                color = color,
                topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.33f),
                size = androidx.compose.ui.geometry.Size(radius * 1.7f, radius * 0.66f),
                style = Stroke(width = radius * 0.11f)
            )
        }

        drawCircle(color = color, radius = radius * 0.23f, center = center)
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = radius * 0.1f,
            center = Offset(center.x - radius * 0.08f, center.y - radius * 0.08f)
        )
    }
}
