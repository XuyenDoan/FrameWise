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

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
