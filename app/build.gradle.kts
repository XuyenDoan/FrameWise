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

    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation-compose").get())
    implementation(libs.findLibrary("androidx-hilt-navigation-compose").get())
}
