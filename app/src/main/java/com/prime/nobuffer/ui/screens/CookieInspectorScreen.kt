package com.prime.nobuffer.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion

@Composable
fun CookieInspectorScreen(
    url: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors
    val cookies = remember(url) { cookieEntriesFor(url) }
    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cookies",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.elevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Close", color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = url.ifBlank { "No page" },
            color = colors.textMid,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Box(modifier = Modifier.size(12.dp))

        if (cookies.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No cookies for this site", color = colors.textDim, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(cookies) { (name, value) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.size(4.dp))
                        SelectionContainer {
                            Text(value.ifBlank { "(empty)" }, color = colors.textMid, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        HomeIndicator()
    }
}
