plugins {
    id("framewise.android.library")
}

android {
    namespace = "com.framewise.core.common"
}

dependencies {
    implementation(libs.findLibrary("kotlinx-coroutines-android").get())
    implementation("javax.inject:javax.inject:1")
}
