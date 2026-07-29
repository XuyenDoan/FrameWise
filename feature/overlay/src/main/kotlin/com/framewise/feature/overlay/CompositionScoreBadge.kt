package com.framewise.feature.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CompositionScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 85 -> Color(0xFF3DDC84)
        score >= 60 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }

    Text(
        text = "$score/100",
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
