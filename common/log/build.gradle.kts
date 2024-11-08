plugins {
    alias(libs.plugins.android.library)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.ksp)
}

android {
    namespace = "sh.kau.playground.common.log"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // allow kotlin auto-complete in ide
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(libs.logcat)

    implementation(libs.androidx.core.ktx)

    // dependency injection
    ksp(libs.kotlin.inject.compiler)
    implementation(libs.kotlin.inject.runtime)
    ksp(libs.kotlin.inject.anvil.compiler)
    implementation(libs.kotlin.inject.anvil.runtime)
    implementation(libs.kotlin.inject.anvil.runtime.utils)

    // internal
    implementation(project(":domain:shared"))
}
