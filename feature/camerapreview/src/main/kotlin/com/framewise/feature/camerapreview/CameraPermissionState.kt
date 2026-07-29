package com.framewise.feature.camerapreview

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Minimal permission-state holder for CAMERA. Kept local to this feature
 * rather than pulling in Accompanist Permissions, since a single permission
 * with a simple grant/deny flow doesn't need that dependency.
 */
@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    return remember(hasPermission) {
        CameraPermissionState(
            hasPermission = hasPermission,
            requestPermission = { launcher.launch(Manifest.permission.CAMERA) },
        )
    }
}

data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)
