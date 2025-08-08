plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.parent?.name}.${project.parent?.name}.api"
}

dependencies {
  // --- project dependencies
}