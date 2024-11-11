plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // internal
  implementation(project(":domain:ui"))
}
