plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // dependency injection
//    ksp(libs.bundles.kotlin.inject.compiler)
//    implementation(libs.bundles.kotlin.inject)
    // networking implementation
//    implementation(platform((libs.ktor.bom)))
//    api(libs.bundles.ktor) // need ktor classes (outside of NetworkApi)

    // --- project dependencies
    api(projects.common.usf.api)
//    implementation(projects.common.log)
}
