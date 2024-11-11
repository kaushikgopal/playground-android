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

// common deps shared across the app but swappable with other implementations
include(
    ":common:log",   // android non-ui (compose) module
)

// common deps shared across the app but domain specific and only used in this app
include(
    ":domain:shared", // pure kotlin module |  (like @Named for DI usage)
    ":domain:ui",     // compose theme & style shared
)

// features of this app; each standalone
include(
    ":features:landing", // android ui (compose) module
    ":features:settings",
)