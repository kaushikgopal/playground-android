plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // Navigation
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)
    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)


    // internal
    api(projects.common.log)
    api(projects.domain.ui) // brings in compose
}
