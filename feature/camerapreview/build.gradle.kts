plugins {
    id("framewise.android.feature")
}

android {
    namespace = "com.framewise.feature.camerapreview"
}

dependencies {
    // Narrow, documented exception to "feature depends on domain only":
    // see CameraPreviewBinder.kt in data/camera for why the preview surface
    // wiring can't be abstracted through domain.
    implementation(project(":data:camera"))

    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
}
