plugins {
  id("com.android.library")
  id("template.android")
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  implementation(platform((libs.ktor.bom)))
  implementation(libs.bundles.ktor)

  // Navigation
  implementation(libs.compose.navigation)
  implementation(libs.kotlinx.serialization.json)
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  // --- internal dependencies
  implementation(projects.common.log)
}
