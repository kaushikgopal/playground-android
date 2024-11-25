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
  // dependencies you want most other feature modules to have
  // be judicious
  api(platform(libs.compose.bom))
  api(libs.bundles.compose)
  api(libs.compose.tooling.preview)  // AndroidStudio Preview support
  debugApi(libs.compose.tooling)

  // --- project dependencies
  implementation(projects.domain.shared)
}
