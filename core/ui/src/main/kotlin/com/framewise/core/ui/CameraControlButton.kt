package com.framewise.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Small translucent circular icon button used for the secondary camera
 * controls row (flash, lens switch, grid toggle, settings) that sits above
 * the shutter, out of the way of the live preview.
 */
@Composable
fun CameraControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(color = Color.Black.copy(alpha = 0.35f), shape = CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
