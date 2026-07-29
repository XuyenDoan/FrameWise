plugins {
    id("framewise.android.library")
    id("framewise.android.compose")
}

android {
    namespace = "com.framewise.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
}
