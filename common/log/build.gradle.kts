plugins {
    alias(libs.plugins.android.library)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.ksp)
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
    }

    // allow kotlin auto-complete in ide
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(libs.logcat)

    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)

    // internal
    implementation(project(":domain:shared"))
}
