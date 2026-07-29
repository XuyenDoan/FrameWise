package com.framewise.feature.camerapreview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Before/after" is interpreted here as comparing composition scores across
 * capture attempts within the same session, rather than an AI-suggested
 * recomposition of a single photo (which would need a generative model,
 * out of scope for this phase) — the two most recent shots side by side,
 * plus the full session history below.
 */
@Composable
fun CaptureHistoryPanel(history: List<CaptureHistoryEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Lịch sử chụp trong phiên này",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )

        if (history.isEmpty()) {
            Text(
                text = "Chưa có ảnh nào được chụp.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            return@Column
        }

        if (history.size >= 2) {
            Text(
                text = "So sánh 2 lần gần nhất",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureThumbnail(entry = history[1], label = "Trước", modifier = Modifier.weight(1f))
                CaptureThumbnail(entry = history[0], label = "Sau", modifier = Modifier.weight(1f))
            }
        }

        Text(
            text = "Toàn bộ (${history.size})",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(history) { entry ->
                HistoryRow(entry)
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: CaptureHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.uri.substringAfterLast('/'),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = "${entry.compositionScore}/100",
            color = scoreColor(entry.compositionScore),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CaptureThumbnail(entry: CaptureHistoryEntry, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)

        val context = LocalContext.current
        val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = entry.uri) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(entry.uri))?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = bitmapState.value
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            }
        }

        Text(
            text = "${entry.compositionScore}/100",
            color = scoreColor(entry.compositionScore),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF3DDC84)
    score >= 60 -> Color(0xFFFFB300)
    else -> Color(0xFFFF5252)
}
