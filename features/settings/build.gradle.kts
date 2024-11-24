plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // Navigation
  implementation(libs.compose.navigation)
  implementation(libs.kotlinx.serialization.json)
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  // --- internal dependencies
  implementation(projects.domain.app)
  implementation(projects.domain.quoter.impl)
}
