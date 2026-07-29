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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.framewise.core.ui.CameraControlButton
import com.framewise.core.ui.ShutterButton
import com.framewise.domain.model.DetectedSubject
import com.framewise.domain.model.FlashMode
import com.framewise.domain.model.GridType
import com.framewise.domain.model.GuidanceType
import com.framewise.domain.model.SceneType
import com.framewise.domain.model.ShootingMode
import com.framewise.feature.overlay.ArGuidanceArrow
import com.framewise.feature.overlay.BoundingBoxOverlay
import com.framewise.feature.overlay.CompositionScoreBadge
import com.framewise.feature.overlay.GridOverlay
import com.framewise.feature.overlay.GuidanceBanner
import com.framewise.feature.overlay.HorizonLevelOverlay
import com.framewise.feature.overlay.HorizonLineOverlay
import com.framewise.feature.overlay.PhotographyTipCaption
import com.framewise.feature.overlay.PoseGuidanceBanner
import com.framewise.feature.overlay.SceneBadge

@Composable
fun CameraPreviewRoute(
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val captureHistory by viewModel.captureHistory.collectAsState()
    val permissionState = rememberCameraPermissionState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (!permissionState.hasPermission) {
            permissionState.requestPermission()
        }
    }

    // Previously nothing collected captureEvents at all, so a successful
    // (or failed) capture gave the user zero feedback - the shutter button
    // looked broken even though CameraXController was actually saving a
    // photo every time.
    LaunchedEffect(viewModel) {
        viewModel.captureEvents.collect { event ->
            val message = when (event) {
                is CaptureEvent.Success -> "Đã lưu ảnh vào thư viện"
                is CaptureEvent.Failure -> "Chụp ảnh thất bại: ${event.reason}"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (permissionState.hasPermission) {
            CameraPreviewScreen(
                uiState = uiState,
                captureHistory = captureHistory,
                onBindPreview = viewModel::bindPreview,
                onUnbindPreview = viewModel::unbindPreview,
                onToggleLens = viewModel::onToggleLens,
                onToggleFlash = viewModel::onToggleFlash,
                onZoomChange = viewModel::onZoomChange,
                onExposureChange = viewModel::onExposureChange,
                onFocusTap = viewModel::onFocusTap,
                onGridTypeSelected = viewModel::onGridTypeSelected,
                onToggleVoice = viewModel::onToggleVoice,
                onShootingModeSelected = viewModel::onShootingModeSelected,
                onSubjectTapped = viewModel::onSubjectTapped,
                onCapture = viewModel::onCapture,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CameraPermissionRationale(
                onRequestPermission = permissionState.requestPermission,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun CameraPreviewScreen(
    uiState: CameraPreviewUiState,
    captureHistory: List<CaptureHistoryEntry>,
    onBindPreview: (LifecycleOwner, PreviewView) -> Unit,
    onUnbindPreview: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleFlash: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onExposureChange: (Int) -> Unit,
    onFocusTap: (Float, Float) -> Unit,
    onGridTypeSelected: (GridType) -> Unit,
    onToggleVoice: () -> Unit,
    onShootingModeSelected: (ShootingMode) -> Unit,
    onSubjectTapped: (DetectedSubject) -> Unit,
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
    val latestSubjects = rememberUpdatedState(uiState.subjects)
    var showHistory by remember { mutableStateOf(false) }
    var showGlossary by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    onZoomChange(latestZoomRatio.value * zoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // A tap first checks whether it landed inside a detected
                    // subject's box (so the user can pick who/what to focus
                    // on when several people/objects are in frame). Hitting
                    // one both selects it for composition guidance AND
                    // drives real camera autofocus to its center - a manual
                    // tap-to-focus at that same point would do the latter
                    // anyway, so this makes "chọn chủ thể" actually focus
                    // the lens on them instead of only updating guidance.
                    val tappedSubject = latestSubjects.value.firstOrNull { subject ->
                        val box = subject.boundingBox
                        offset.x in (box.left * size.width)..(box.right * size.width) &&
                            offset.y in (box.top * size.height)..(box.bottom * size.height)
                    }
                    if (tappedSubject != null) {
                        onSubjectTapped(tappedSubject)
                        val box = tappedSubject.boundingBox
                        onFocusTap(box.centerX * size.width, box.centerY * size.height)
                    } else {
                        onFocusTap(offset.x, offset.y)
                    }
                }
            },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        GridOverlay(gridType = uiState.gridType, modifier = Modifier.fillMaxSize())
        HorizonLineOverlay(horizonLineY = uiState.horizonLineY, modifier = Modifier.fillMaxSize())
        BoundingBoxOverlay(
            subjects = uiState.subjects,
            selectedSubjectKey = uiState.selectedSubjectKey,
            modifier = Modifier.fillMaxSize(),
        )

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
                    val density = LocalDensity.current
                    Box {
                        CameraControlButton(
                            icon = Icons.Filled.GridOn,
                            contentDescription = "Grid",
                            onClick = { showGridPicker = !showGridPicker },
                        )
                        if (showGridPicker) {
                            // Popup floats above the surface instead of participating
                            // in this Row's layout, so it can never push the score
                            // badge / lens-switch button off narrow screens the way an
                            // inline LazyRow did.
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(0, with(density) { 56.dp.roundToPx() }),
                                onDismissRequest = { showGridPicker = false },
                            ) {
                                GridTypePicker(
                                    selected = uiState.gridType,
                                    onSelected = { type ->
                                        onGridTypeSelected(type)
                                        showGridPicker = false
                                    },
                                )
                            }
                        }
                    }
                    CameraControlButton(
                        icon = if (uiState.isVoiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Voice guidance",
                        onClick = onToggleVoice,
                    )
                    CameraControlButton(
                        icon = Icons.Filled.History,
                        contentDescription = "Capture history",
                        onClick = { showHistory = !showHistory },
                    )
                }

                CompositionScoreBadge(score = uiState.compositionScore)

                CameraControlButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    onClick = onToggleLens,
                )
            }

            ShootingModeSelector(
                selected = uiState.shootingMode,
                onSelected = onShootingModeSelected,
                onShowGlossary = { showGlossary = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                HorizonLevelOverlay(horizonState = uiState.horizonState)
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.scene != SceneType.UNKNOWN) {
                        SceneBadge(scene = uiState.scene)
                        if (uiState.photographyTip.isNotBlank()) {
                            PhotographyTipCaption(tip = uiState.photographyTip)
                        }
                    }
                    ArGuidanceArrow(
                        guidance = uiState.guidanceMessages.minByOrNull { it.priority } ?: GuidanceType.GOOD,
                    )
                    GuidanceBanner(messages = uiState.guidanceMessages)
                    PoseGuidanceBanner(suggestions = uiState.poseSuggestions)
                }
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

        if (showHistory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(Unit) { detectTapGestures { showHistory = false } },
                contentAlignment = Alignment.Center,
            ) {
                CaptureHistoryPanel(
                    history = captureHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }

        if (showGlossary) {
            PhotographyGlossaryDialog(onDismiss = { showGlossary = false })
        }
    }
}

@Composable
private fun GridTypePicker(
    selected: GridType,
    onSelected: (GridType) -> Unit,
    modifier: Modifier = Modifier,
) {
    // widthIn(max) instead of a fixed width: caps how wide this can get on
    // small phones while still shrinking to fit on any screen; the LazyRow
    // inside scrolls internally if all chips still don't fit at max width.
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
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
}

private fun GridType.toVietnameseLabel(): String = when (this) {
    GridType.NONE -> "Tắt"
    GridType.RULE_OF_THIRDS -> "1/3"
    GridType.GOLDEN_RATIO -> "Tỷ lệ vàng"
    GridType.GOLDEN_TRIANGLE -> "Tam giác vàng"
    GridType.SQUARE -> "Ô vuông"
    GridType.DIAGONAL -> "Đường chéo"
}

/**
 * A manual mode toggle (not an inline popup like [GridTypePicker]) since
 * switching mode is a frequent, primary action while shooting - always
 * visible, one row, scrolls horizontally on narrow screens instead of a
 * fixed width (same overflow-safety lesson as the GridTypePicker fix).
 */
@Composable
private fun ShootingModeSelector(
    selected: ShootingMode,
    onSelected: (ShootingMode) -> Unit,
    onShowGlossary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(ShootingMode.entries) { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                label = { Text(mode.toVietnameseLabel()) },
            )
        }
        item {
            CameraControlButton(
                icon = Icons.Filled.HelpOutline,
                contentDescription = "Giải thích quy tắc bố cục",
                onClick = onShowGlossary,
            )
        }
    }
}

private fun ShootingMode.toVietnameseLabel(): String = when (this) {
    ShootingMode.AUTO -> "Tự động"
    ShootingMode.PORTRAIT -> "Chân dung"
    ShootingMode.ANIMAL -> "Thú vật"
    ShootingMode.LANDSCAPE -> "Phong cảnh"
}

/**
 * Static, offline glossary - not a generative AI call. Explanations are
 * short, fixed text a beginner can read without leaving the camera screen.
 */
@Composable
private fun PhotographyGlossaryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Đã hiểu") }
        },
        title = { Text("Các khái niệm cơ bản") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlossaryEntry(
                    term = "Quy tắc 1/3",
                    explanation = "Chia khung hình thành 9 ô bằng 2 đường ngang và 2 đường dọc. " +
                        "Đặt chủ thể chính (mắt, mặt người...) vào 1 trong 4 giao điểm thay vì " +
                        "giữa khung sẽ giúp ảnh tự nhiên và cân đối hơn.",
                )
                GlossaryEntry(
                    term = "Tỷ lệ vàng",
                    explanation = "Một biến thể của quy tắc 1/3 nhưng các đường chia theo tỷ lệ " +
                        "1:1.618 (tỷ lệ thường gặp trong tự nhiên) thay vì chia đều 3 phần " +
                        "bằng nhau - tạo cảm giác hài hoà hơn một chút, thường dùng cho ảnh " +
                        "phong cảnh/kiến trúc.",
                )
                GlossaryEntry(
                    term = "Khoảng trống đầu (headroom)",
                    explanation = "Khoảng cách từ đỉnh đầu chủ thể đến mép trên khung hình khi " +
                        "chụp chân dung. Quá ít sẽ thấy chật chội, quá nhiều sẽ mất cân đối - " +
                        "app tự tính khoảng trống lý tưởng khi phát hiện khuôn mặt.",
                )
                GlossaryEntry(
                    term = "Đường chân trời",
                    explanation = "Ranh giới trời và đất/biển trong ảnh phong cảnh. Nên giữ " +
                        "thẳng tuyệt đối (không nghiêng) và đặt gần đường 1/3 trên hoặc dưới, " +
                        "tránh đặt đúng giữa khung.",
                )
                GlossaryEntry(
                    term = "Chế độ chụp (Chân dung/Thú vật/Phong cảnh)",
                    explanation = "Chọn đúng chế độ giúp app ưu tiên đúng quy tắc: Chân dung/Thú " +
                        "vật chú trọng lấy nét đúng chủ thể + hậu cảnh gọn; Phong cảnh chú " +
                        "trọng đường chân trời. Chạm vào khung quanh người/vật trên màn hình " +
                        "để chọn đúng chủ thể muốn lấy nét khi có nhiều người/vật trong khung.",
                )
            }
        },
    )
}

@Composable
private fun GlossaryEntry(term: String, explanation: String) {
    Column {
        Text(text = term, style = MaterialTheme.typography.titleSmall)
        Text(text = explanation, style = MaterialTheme.typography.bodySmall)
    }
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
