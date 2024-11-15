plugins {
  id("com.android.application")
  id("template.feature") // comes packed with a lot of feature even at app level
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
}

dependencies {

  // internal
  implementation(project(":domain:ui"))
  implementation(project(":domain:shared"))

  implementation(project(":features:landing"))
  implementation(project(":features:settings"))

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.espresso.core)
  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.test.junit4)
  debugImplementation(libs.compose.test.manifest)
}
