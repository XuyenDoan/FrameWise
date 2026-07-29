package com.framewise.feature.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.framewise.domain.model.SceneType

/** Small pill showing the currently recognized scene, e.g. "Phong cảnh". */
@Composable
fun SceneBadge(scene: SceneType, modifier: Modifier = Modifier) {
    if (scene == SceneType.UNKNOWN) return

    Text(
        text = scene.toVietnameseLabel(),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Tip text tied to the current scene, shown as a small caption strip. */
@Composable
fun PhotographyTipCaption(tip: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = tip,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun SceneType.toVietnameseLabel(): String = when (this) {
    SceneType.PORTRAIT -> "Chân dung"
    SceneType.LANDSCAPE -> "Phong cảnh"
    SceneType.FOOD -> "Đồ ăn"
    SceneType.ARCHITECTURE -> "Kiến trúc"
    SceneType.PET -> "Thú cưng"
    SceneType.FLOWER -> "Hoa"
    SceneType.SUNSET -> "Hoàng hôn"
    SceneType.NIGHT -> "Ban đêm"
    SceneType.STREET -> "Đường phố"
    SceneType.MACRO -> "Macro"
    SceneType.UNKNOWN -> ""
}
