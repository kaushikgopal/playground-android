plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.android.lint)
}

lint {
  htmlReport = true
  htmlOutput = file("lint-report.html")
  textReport = true
  absolutePaths = false
  ignoreTestSources = true
}

dependencies {
  compileOnly(libs.bundles.lint.api)
  // testImplementation(libs.bundles.lint.tests)
}
