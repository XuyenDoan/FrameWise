// Top-level build file. Actual plugin versions are resolved through the
// included build-logic convention plugins (see build-logic/) so every
// module configures Android/Kotlin/Hilt/Compose the same way.

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
