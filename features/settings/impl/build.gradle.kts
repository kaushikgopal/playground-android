plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.parent?.name}.${project.parent?.name}.impl"
}

dependencies {
  // --- project dependencies
  api(projects.features.settings.api)
  api(projects.domain.quoter.api)
  /* implementation(projects.domain.quoter.impl) */ // assembling happens in AppComponent
}