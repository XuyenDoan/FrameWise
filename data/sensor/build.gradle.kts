plugins {
    id("framewise.android.library")
    id("framewise.android.hilt")
}

android {
    namespace = "com.framewise.data.sensor"
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.findLibrary("kotlinx-coroutines-android").get())
}
