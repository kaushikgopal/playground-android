plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.compose")
}

android {
    namespace = "sh.kau.playground.features.landing"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }
    kotlinOptions {
        // allow kotlin auto-complete in ide
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)

    // dependency injection
    ksp(libs.kotlin.inject.compiler)
    implementation(libs.kotlin.inject.runtime)
    ksp(libs.kotlin.inject.anvil.compiler)
    implementation(libs.kotlin.inject.anvil.runtime)
    implementation(libs.kotlin.inject.anvil.runtime.utils)

    // navigation
    implementation(libs.androidx.navigation)
    implementation(libs.kotlinx.serialization.json)

    // Compose Bill Of Materials
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics) //
    implementation(libs.androidx.ui.tooling.preview) // Android Studio Preview support
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    // internal

}
