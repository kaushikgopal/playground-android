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
  // navigation
  implementation(libs.compose.navigation)
  implementation(libs.kotlinx.serialization.json)
  // dependency injection
  ksp(libs.bundles.kotlin.inject.compiler)
  implementation(libs.bundles.kotlin.inject)

  // --- project dependencies
  implementation(projects.domain.app)
  implementation(projects.features.landing)
  implementation(projects.features.settings)

  // --- testing
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.espresso.core)
  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.test.junit4)
  debugImplementation(libs.compose.test.manifest)
}
