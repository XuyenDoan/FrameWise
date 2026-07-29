package com.framewise.feature.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framewise.domain.model.DetectedSubject

@Composable
fun BoundingBoxOverlay(subjects: List<DetectedSubject>, modifier: Modifier = Modifier) {
    if (subjects.isEmpty()) return

    Canvas(modifier = modifier) {
        subjects.forEach { subject ->
            val box = subject.boundingBox
            drawRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(box.left * size.width, box.top * size.height),
                size = Size(box.width * size.width, box.height * size.height),
                style = Stroke(width = 3f),
            )
        }
    }
}
