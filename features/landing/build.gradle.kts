plugins {
    id("template.feature")
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.compose")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // navigation
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)

    // Compose Bill Of Materials
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.tools.graphics) //
    implementation(libs.compose.tools.preview) // Android Studio Preview support
    implementation(libs.compose.material)
    debugImplementation(libs.compose.tools)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    // internal

}
