plugins {
    id("com.android.library")
    id("template.android")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)
    // should be removed with kotlin-inject-anvil
    implementation(platform((libs.ktor.bom)))
    implementation(libs.bundles.ktor)

    // --- project dependencies
    api(projects.domain.quoter.api)
    api(projects.common.networking) // need ktor classes (outside of NetworkApi)
}