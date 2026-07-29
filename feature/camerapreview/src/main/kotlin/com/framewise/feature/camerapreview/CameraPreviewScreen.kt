package com.framewise.feature.camerapreview

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.framewise.core.ui.CameraControlButton
import com.framewise.core.ui.ShutterButton
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.GridType
import com.framewise.feature.overlay.BoundingBoxOverlay
import com.framewise.feature.overlay.CompositionScoreBadge
import com.framewise.feature.overlay.GridOverlay
import com.framewise.feature.overlay.GuidanceBanner
import com.framewise.feature.overlay.HorizonLevelOverlay

@Composable
fun CameraPreviewRoute(
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionState = rememberCameraPermissionState()

    LaunchedEffect(Unit) {
        if (!permissionState.hasPermission) {
            permissionState.requestPermission()
        }
    }

    if (permissionState.hasPermission) {
        CameraPreviewScreen(
            uiState = uiState,
            onBindPreview = viewModel::bindPreview,
            onUnbindPreview = viewModel::unbindPreview,
            onToggleLens = viewModel::onToggleLens,
            onToggleFlash = viewModel::onToggleFlash,
            onZoomChange = viewModel::onZoomChange,
            onExposureChange = viewModel::onExposureChange,
            onFocusTap = viewModel::onFocusTap,
            onGridTypeSelected = viewModel::onGridTypeSelected,
            onCapture = viewModel::onCapture,
            modifier = modifier,
        )
    } else {
        CameraPermissionRationale(
            onRequestPermission = permissionState.requestPermission,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraPreviewScreen(
    uiState: CameraPreviewUiState,
    onBindPreview: (LifecycleOwner, PreviewView) -> Unit,
    onUnbindPreview: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleFlash: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onExposureChange: (Int) -> Unit,
    onFocusTap: (Float, Float) -> Unit,
    onGridTypeSelected: (GridType) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    DisposableEffectPreviewBinding(
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        onBind = onBindPreview,
        onUnbind = onUnbindPreview,
    )

    val latestZoomRatio = rememberUpdatedState(uiState.zoomRatio)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onZoomChange(latestZoomRatio.value * zoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onFocusTap(offset.x, offset.y) }
            },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        GridOverlay(gridType = uiState.gridType, modifier = Modifier.fillMaxSize())
        BoundingBoxOverlay(subjects = uiState.subjects, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraControlButton(
                        icon = flashIconFor(uiState.flashMode),
                        contentDescription = "Flash",
                        onClick = onToggleFlash,
                    )
                    var showGridPicker by remember { mutableStateOf(false) }
                    CameraControlButton(
                        icon = Icons.Filled.GridOn,
                        contentDescription = "Grid",
                        onClick = { showGridPicker = !showGridPicker },
                    )
                    if (showGridPicker) {
                        GridTypePicker(
                            selected = uiState.gridType,
                            onSelected = onGridTypeSelected,
                        )
                    }
                }

                CompositionScoreBadge(score = uiState.compositionScore)

                CameraControlButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    onClick = onToggleLens,
                )
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                HorizonLevelOverlay(horizonState = uiState.horizonState)
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                GuidanceBanner(messages = uiState.guidanceMessages)
            }

            ExposureSlider(
                exposureIndex = uiState.exposureIndex,
                exposureRange = uiState.exposureRange,
                onExposureChange = onExposureChange,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (uiState.isCapturing) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    ShutterButton(onClick = onCapture, enabled = uiState.isReady)
                }
            }
        }
    }
}

@Composable
private fun GridTypePicker(
    selected: GridType,
    onSelected: (GridType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.width(280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(GridType.entries) { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(type.toVietnameseLabel()) },
            )
        }
    }
}

private fun GridType.toVietnameseLabel(): String = when (this) {
    GridType.NONE -> "Tắt"
    GridType.RULE_OF_THIRDS -> "1/3"
    GridType.GOLDEN_RATIO -> "Tỷ lệ vàng"
    GridType.GOLDEN_TRIANGLE -> "Tam giác vàng"
    GridType.SQUARE -> "Ô vuông"
    GridType.DIAGONAL -> "Đường chéo"
}

@Composable
private fun ExposureSlider(
    exposureIndex: Int,
    exposureRange: IntRange,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (exposureRange.first == exposureRange.last) return

    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Phơi sáng: $exposureIndex",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        Slider(
            value = exposureIndex.toFloat(),
            onValueChange = { onExposureChange(it.toInt()) },
            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
            steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun DisposableEffectPreviewBinding(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onBind: (LifecycleOwner, PreviewView) -> Unit,
    onUnbind: () -> Unit,
) {
    DisposableEffect(lifecycleOwner) {
        onBind(lifecycleOwner, previewView)
        onDispose { onUnbind() }
    }
}

@Composable
private fun flashIconFor(flashMode: FlashMode) = when (flashMode) {
    FlashMode.OFF -> Icons.Filled.FlashOff
    FlashMode.AUTO -> Icons.Filled.FlashAuto
    FlashMode.ON, FlashMode.TORCH -> Icons.Filled.FlashOn
}

@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FrameWise cần quyền truy cập Camera để hoạt động",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Cấp quyền Camera")
            }
        }
    }
}
