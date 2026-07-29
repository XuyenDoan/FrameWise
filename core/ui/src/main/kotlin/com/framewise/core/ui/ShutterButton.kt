package com.framewise.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * The single primary capture control. Deliberately plain — a ring plus a
 * solid disc, no icon — matching the minimal shutter buttons of dedicated
 * camera hardware rather than a generic Material icon button.
 */
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(72.dp)
            .semantics { contentDescription = "Shutter" },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .border(width = 3.dp, color = Color.White, shape = CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(color = Color.White, shape = CircleShape),
            )
        }
    }
}

@Preview
@Composable
private fun ShutterButtonPreview() {
    MaterialTheme {
        ShutterButton(onClick = {})
    }
}
