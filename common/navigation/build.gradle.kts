plugins {
  id("com.android.library")
  id("template.android")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  api(libs.jetpack.navigation3.runtime)
  api(libs.jetpack.navigation3.ui)
  
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)
  
  implementation(project(":domain:shared"))
}