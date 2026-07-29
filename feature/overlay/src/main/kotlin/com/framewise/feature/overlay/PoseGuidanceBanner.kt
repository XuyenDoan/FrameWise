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
import com.framewise.domain.model.PoseSuggestion

/**
 * Shown only when there's an actual suggestion — an empty/NONE pose result
 * (no person, or a well-composed pose already) renders nothing, matching
 * [GuidanceBanner]'s "one clear instruction, not a checklist" approach.
 */
@Composable
fun PoseGuidanceBanner(suggestions: List<PoseSuggestion>, modifier: Modifier = Modifier) {
    val top = suggestions.firstOrNull { it != PoseSuggestion.NONE } ?: return

    Text(
        text = top.toVietnameseLabel(),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .background(color = Color(0xFFFFB300).copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

private fun PoseSuggestion.toVietnameseLabel(): String = when (this) {
    PoseSuggestion.RELAX_SHOULDERS -> "Thả lỏng vai, giữ vai ngang bằng"
    PoseSuggestion.RAISE_CHIN -> "Ngẩng cằm lên một chút"
    PoseSuggestion.NONE -> ""
}
