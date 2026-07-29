plugins {
    id("framewise.android.library")
    id("framewise.android.hilt")
}

android {
    namespace = "com.framewise.data.camera"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))

    implementation(libs.findLibrary("androidx-camera-core").get())
    implementation(libs.findLibrary("androidx-camera-camera2").get())
    implementation(libs.findLibrary("androidx-camera-lifecycle").get())
    implementation(libs.findLibrary("androidx-camera-view").get())

    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("kotlinx-coroutines-android").get())
}
