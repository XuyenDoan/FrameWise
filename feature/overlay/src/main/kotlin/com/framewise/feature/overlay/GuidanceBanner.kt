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
import com.framewise.domain.model.GuidanceType

/**
 * Shows only the single highest-priority guidance message (see
 * [GuidanceType.priority]) rather than listing every imperfection at once —
 * a photographer mid-frame needs one clear instruction, not a checklist.
 */
@Composable
fun GuidanceBanner(messages: List<GuidanceType>, modifier: Modifier = Modifier) {
    val topMessage = messages.minByOrNull { it.priority } ?: GuidanceType.GOOD

    Text(
        text = topMessage.toVietnameseLabel(),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun GuidanceType.toVietnameseLabel(): String = when (this) {
    GuidanceType.LEVEL_HORIZON -> "Cân bằng đường chân trời"
    GuidanceType.MOVE_LEFT -> "Di chuyển sang trái"
    GuidanceType.MOVE_RIGHT -> "Di chuyển sang phải"
    GuidanceType.RAISE_CAMERA -> "Nâng camera lên"
    GuidanceType.LOWER_CAMERA -> "Hạ camera xuống"
    GuidanceType.MOVE_CLOSER -> "Tiến gần hơn"
    GuidanceType.MOVE_FARTHER -> "Lùi ra xa"
    GuidanceType.IMPROVE_LIGHTING -> "Điều chỉnh ánh sáng"
    GuidanceType.BUSY_BACKGROUND -> "Hậu cảnh hơi rối, thử đổi góc chụp"
    GuidanceType.GOOD -> "Bố cục đẹp"
}
