plugins {
    id("framewise.android.library")
}

android {
    namespace = "com.framewise.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation("javax.inject:javax.inject:1")
}
