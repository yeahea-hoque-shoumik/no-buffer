package com.prime.nobuffer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.ui.theme.Orion

/**
 * Home content only — no StatusBar/PillBar/HomeIndicator. Meant to be overlaid inside
 * BrowserScreen's content area so the pill bar never remounts when navigating home <-> a page.
 */
@Composable
fun IncognitoNewTabContent(modifier: Modifier = Modifier) {
    val colors = Orion.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🥸", fontSize = 48.sp)
        Box(modifier = Modifier.size(16.dp))
        Text(
            text = "You've gone incognito",
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Box(modifier = Modifier.size(24.dp))

        IncognitoBullet(glyph = "✓", text = "No history, autofill, downloads, or recently-closed entries")
        IncognitoBullet(glyph = "✓", text = "Site storage stays in memory and is discarded with the tab")
        IncognitoBullet(glyph = "✓", text = "Cookies for private-only sites are cleared when the last private tab closes")
        IncognitoBullet(glyph = "✗", text = "Websites, your network, and Android itself can still see this session")
    }
}

@Composable
private fun IncognitoBullet(glyph: String, text: String) {
    val colors = Orion.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(glyph, color = colors.textMid, fontSize = 13.sp)
        Box(modifier = Modifier.size(10.dp))
        Text(text, color = colors.textMid, fontSize = 13.sp)
    }
}
