plugins {
  id("com.android.application")
  id("template.android")
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = libs.versions.app.namespace.get()

  defaultConfig {
    applicationId = libs.versions.app.namespace.get()
    versionCode = libs.versions.app.version.code.get().toInt()
    versionName = libs.versions.app.version.name.get()
    targetSdk = libs.versions.sdk.target.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures { compose = true }

  lint {
    quiet = true
    // if true, stop the gradle build if errors are found
    abortOnError = true
    // if true, only report errors
    ignoreWarnings = true
    // Produce report for CI:
    // https://docs.github.com/en/github/finding-security-vulnerabilities-and-errors-in-your-code/sarif-support-for-code-scanning
    // sarifOutput = file("../lint-results.sarif")
    textReport = true
  }
}

dependencies {
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  implementation(projects.common.navigation)
  // --- project dependencies
  implementation(projects.features.landing)
  implementation(projects.features.settings.impl)
  /**/ implementation(projects.domain.quoter.api)
  /**/ androidTestImplementation(projects.domain.quoter.api)
  /**/ implementation(projects.domain.quoter.impl)
  /**/ androidTestImplementation(projects.domain.quoter.impl)

  // --- testing
  testImplementation(libs.bundles.testing)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.espresso.core)
  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.test.junit4)
  debugImplementation(libs.compose.test.manifest)
}
