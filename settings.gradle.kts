pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FrameWise"

include(":app")

include(":core:common")
include(":core:designsystem")
include(":core:ui")

include(":domain")

include(":data:camera")
include(":data:sensor")
include(":data:vision")

include(":feature:overlay")
include(":feature:camerapreview")
