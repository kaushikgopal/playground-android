plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // --- project dependencies
  implementation(project(":common:navigation"))
  implementation(project(":features:settings")) // for navigation to settings
}
