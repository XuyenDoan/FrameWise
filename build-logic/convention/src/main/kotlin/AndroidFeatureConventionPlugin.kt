import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention for `feature:*` modules: Android library + Compose + Hilt,
 * plus the project dependencies every feature module needs (domain layer
 * and shared UI/design-system modules), so feature build.gradle.kts files
 * stay a few lines long.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("framewise.android.library")
                apply("framewise.android.compose")
                apply("framewise.android.hilt")
            }

            dependencies {
                add("implementation", project(":domain"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))

                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            }
        }
    }
}
