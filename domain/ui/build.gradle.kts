import utils.libs

plugins {
  id("com.android.library")
  id("template.android")
  alias(libs.plugins.kotlin.compose.compiler)
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"

  buildFeatures { compose = true }
}

dependencies {
  // Compose
  // using api (so modules that depend on this one can leverage compose directly)
  api(platform(libs.compose.bom))
  api(libs.bundles.compose)
  // AndroidStudio Preview support
  api(libs.compose.tooling.preview)
  debugApi(libs.compose.tooling)

  // Internal
  implementation(projects.domain.shared)
}
