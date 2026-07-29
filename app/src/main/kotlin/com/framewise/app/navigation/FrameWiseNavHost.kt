package com.framewise.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.framewise.feature.camerapreview.CameraPreviewRoute

@Composable
fun FrameWiseNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FrameWiseDestinations.CAMERA_ROUTE,
        modifier = modifier,
    ) {
        composable(FrameWiseDestinations.CAMERA_ROUTE) {
            CameraPreviewRoute()
        }
    }
}
