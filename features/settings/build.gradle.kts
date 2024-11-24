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
  implementation(projects.domain.quoter.api)
  // but providing implementation through settings comp
  // TODO: kotlin-inject-anvil can remove below need
  implementation(projects.domain.quoter.impl) // usually not required

}
