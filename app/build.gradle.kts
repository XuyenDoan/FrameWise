plugins {
    id("framewise.android.application")
    id("framewise.android.compose")
    id("framewise.android.hilt")
}

android {
    namespace = "com.framewise.app"

    defaultConfig {
        applicationId = "com.framewise.app"
        versionCode = 1
        versionName = "0.1.0-phase2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":data:camera"))
    implementation(project(":feature:camerapreview"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
