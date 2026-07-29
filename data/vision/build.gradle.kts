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

    implementation(libs.findLibrary("androidx-camera-core").get())
    implementation(libs.findLibrary("mlkit-object-detection").get())
    implementation(libs.findLibrary("mlkit-face-detection").get())
    implementation(libs.findLibrary("mlkit-image-labeling").get())
    implementation(libs.findLibrary("kotlinx-coroutines-android").get())
}
