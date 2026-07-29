// Top-level build file. Module configuration itself lives in the included
// build-logic convention plugins (see build-logic/); this "apply false"
// block only exists to resolve AGP/Kotlin/KSP/Hilt/Compose-compiler onto
// the root project's plugin classpath, which is what lets those convention
// plugins call `pluginManager.apply("com.android.application")` etc. at
// the subproject level.
//
// Versions are hardcoded here (matching gradle/libs.versions.toml) rather
// than referenced through the `libs.plugins.*` type-safe accessor, since
// the version-catalog-accessors plugin's generated code isn't reliably
// available yet at this point in the build (root `plugins {}` block is
// evaluated very early, before subproject accessor generation).

plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
