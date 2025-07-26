plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
}

dependencies {
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  implementation(projects.domain.shared)
  api(projects.common.coroutines.api)

  implementation(platform(libs.coroutines.bom))
  implementation(libs.coroutines.core)
}