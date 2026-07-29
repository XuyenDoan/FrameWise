package com.framewise.feature.camerapreview

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
 * Minimal permission-state holder. Kept local to this feature rather than
 * pulling in Accompanist Permissions, since this simple grant/deny flow
 * doesn't need that dependency.
 *
 * Requests CAMERA, plus WRITE_EXTERNAL_STORAGE on API 26-28 only - saving a
 * capture through MediaStore (see CameraXController.capturePhoto) needs
 * that permission pre-Q, but scoped storage makes it unnecessary (and it's
 * not even declared in the manifest past API 28) from Q onward.
 */
@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    fun allGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var hasPermission by remember { mutableStateOf(allGranted()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results -> hasPermission = results.values.all { it } }

    return remember(hasPermission) {
        CameraPermissionState(
            hasPermission = hasPermission,
            requestPermission = { launcher.launch(requiredPermissions.toTypedArray()) },
        )
    }
}

data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)
