plugins {
  id("com.android.library")
  id("template.android")
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.parent?.name}.${project.parent?.name}.api"
}

dependencies {
  // --- project dependencies
  implementation(project(":common:navigation"))
}