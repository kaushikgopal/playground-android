plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // navigation
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)
    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)

    // --- project dependencies
    implementation(projects.common.log)
    implementation(projects.features.settings)
    /**/ api(projects.domain.quoter.api)
    /**/ androidTestApi(projects.domain.quoter.api)
    /**/ implementation(projects.domain.quoter.impl)
    /**/ androidTestImplementation(projects.domain.quoter.impl)
}
