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

// features of this app; each standalone
include(
    ":features:landing", // typical feature
    ":features:settings",
)

// module specific to this app
include(
    ":domain:app", // app level functionality
    // features depend on this module preventing circular dependency over :app

    ":domain:ui", // compose theme & design system
    ":domain:quoter", // produce quotes

    ":domain:shared", // pure kotlin module |  @Named + ConfigComponent + referenced everywhere
)


// common modules (shared across the app) but swappable with other implementations
include(
    ":common:log", // android module
    ":common:networking",
    ":common:lint-rules",
)

