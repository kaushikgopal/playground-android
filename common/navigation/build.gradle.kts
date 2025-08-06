plugins {
  id("com.android.library")
  id("template.android")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  // navigation
  api(libs.bundles.jetpack.navigation)

  // project dependencies
  implementation(projects.domain.shared)
}
