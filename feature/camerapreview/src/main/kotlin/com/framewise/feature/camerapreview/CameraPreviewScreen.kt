package com.framewise.feature.camerapreview

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.framewise.domain.model.FlashMode
import com.framewise.core.ui.CameraControlButton
import com.framewise.core.ui.ShutterButton

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
            },
    ) {
        AndroidView(
            factory = { previewView },
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
            ) {
                CameraControlButton(
                    icon = flashIconFor(uiState.flashMode),
                    contentDescription = "Flash",
                    onClick = onToggleFlash,
                )
                CameraControlButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    onClick = onToggleLens,
                )
            }

            Box(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
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
