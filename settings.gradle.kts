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

// See [app-module-diagram.webp] for visual reference
include(":app")

// common modules (shared across the app) but swappable with other implementations
include(
    ":common:log", // android module
    ":common:lint-rules",
)

// module specific to this app
include(
    ":domain:shared", // pure kotlin module |  @Named + ConfigComponent + referenced everywhere
    ":domain:ui", // compose theme & design system
    ":domain:app", // app level functionality
    // features depend on this module preventing circular dependency over :app
)

// features of this app; each standalone
include(
    ":features:landing", // typical feature
    ":features:settings",
)
