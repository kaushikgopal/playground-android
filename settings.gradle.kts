pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Playground"
include(":app")

// common utils that we can swap out with different implementations
include(
    ":common",
    ":common:log",
)

// domain specific dependencies only used in Pudi app
include(
    ":domain",
    ":domain:shared", // used app wide (like AppComponent)
)