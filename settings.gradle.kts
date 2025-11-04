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

rootProject.name = "Playground"

// See [app-module-diagram.webp] for visual reference
include(":app")

// features of this app; each standalone
include(
    ":features:landing", // typical feature
    ":features:settings:api",
    ":features:settings:impl",
)

// module specific to this app
include(
    ":domain:ui", // compose theme & design system
    ":domain:quoter:api", // produce quotes
    ":domain:quoter:impl",
    ":domain:shared", // pure kotlin module | referenced everywhere e.g. @Named
)

// common modules (shared across the app) but swappable with other implementations
include(
    ":common:log", // android module
    ":common:usf", // jvm module
    ":common:usf:log", // convenience logger for feature modules

    ":common:lint-rules",
    ":common:navigation",
    ":common:networking",
)
