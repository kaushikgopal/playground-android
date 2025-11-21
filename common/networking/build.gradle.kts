plugins {
  id("com.android.library")
  id("template.android")
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // networking implementation
  implementation(platform((libs.ktor.bom)))
  api(libs.bundles.ktor) // need ktor classes (outside of NetworkApi)

  // --- project dependencies
  implementation(projects.common.log)
}
