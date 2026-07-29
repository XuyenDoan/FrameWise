plugins {
    id("framewise.android.library")
    id("framewise.android.compose")
}

android {
    namespace = "com.framewise.feature.overlay"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:designsystem"))
}
