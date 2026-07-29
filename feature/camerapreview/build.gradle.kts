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
    // data:sensor/data:vision expose everything through plain domain
    // interfaces (SensorRepository/VisionRepository) - these two are only
    // added here so Hilt can see their @Binds modules at compile time in
    // this single-app-module setup; the ViewModel itself only ever
    // references the domain interfaces, never these packages directly.
    implementation(project(":data:sensor"))
    implementation(project(":data:vision"))
    implementation(project(":feature:overlay"))

    implementation(libs.findLibrary("androidx-camera-view").get())
    implementation(libs.findLibrary("androidx-compose-material-icons-extended").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
}
