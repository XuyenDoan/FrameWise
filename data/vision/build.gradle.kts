plugins {
    id("framewise.android.library")
    id("framewise.android.hilt")
}

android {
    namespace = "com.framewise.data.vision"
}

dependencies {
    implementation(project(":domain"))
    // Narrow, documented exception (same rationale as feature:camerapreview):
    // registering an ImageAnalysis.Analyzer requires the CameraX use case
    // CameraXController owns; see CameraFrameProvider.kt in data:camera.
    implementation(project(":data:camera"))

    implementation(libs.androidx.camera.core)
    implementation(libs.mlkit.object.detection)
    implementation(libs.mlkit.face.detection)
    implementation(libs.kotlinx.coroutines.android)
}
