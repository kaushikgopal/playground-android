plugins {
    id("com.android.library")
    id("template.android")
//    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)

    implementation(libs.kotlinx.serialization.json)
}