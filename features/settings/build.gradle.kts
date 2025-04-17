plugins {
  id("com.android.library")
  id("template.feature")
}

android {
  namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
  // --- project dependencies
  api(projects.domain.quoter.api)
  /* implementation(projects.domain.quoter.impl) */ // assembling happens in AppComponent
}
