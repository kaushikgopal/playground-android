pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
    ":common:log",   // android non-ui (compose) module
)

// domain specific dependencies only used in this app
include(
    ":domain:shared", // pure kotlin module |  (like @Named for DI usage)
    ":domain:ui",     // compose theme & style shared
)

// features of this app; each standalone
include(
    ":features:landing", // android ui (compose) module
)