package com.framewise.feature.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.trackingKey

@Composable
fun BoundingBoxOverlay(
    subjects: List<DetectedSubject>,
    selectedSubjectKey: String? = null,
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) return

    Canvas(modifier = modifier) {
        subjects.forEach { subject ->
            val box = subject.boundingBox
            val isSelected = selectedSubjectKey != null && subject.trackingKey() == selectedSubjectKey
            drawRect(
                color = if (isSelected) Color(0xFF3DDC84) else Color(0xFFFFB300),
                topLeft = Offset(box.left * size.width, box.top * size.height),
                size = Size(box.width * size.width, box.height * size.height),
                style = Stroke(width = if (isSelected) 6f else 3f),
            )
        }
    }
}
