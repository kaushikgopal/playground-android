plugins {
    id("template.feature")
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.compose")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // navigation
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)

    // internal
}
