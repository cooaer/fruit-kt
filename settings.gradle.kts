pluginManagement {
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

rootProject.name = "fruit-kt"

include(":fruit")
include(":fruit-ksp")
include(":fruit-ktor")
include(":fruit-converter-retrofit")
include(":fruit-ksp-sample")
