// Top-level build file. Module configuration itself lives in the included
// build-logic convention plugins (see build-logic/); this "apply false"
// block only exists to resolve AGP/Kotlin/KSP/Hilt/Compose-compiler onto
// the root project's plugin classpath, which is what lets those convention
// plugins call `pluginManager.apply("com.android.application")` etc. at
// the subproject level.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.compose.compiler) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
