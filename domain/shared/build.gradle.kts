
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
}

dependencies {
    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)
}
