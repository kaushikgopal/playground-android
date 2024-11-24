plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    implementation(projects.domain.quoter.api)


    // dependency injection
    ksp(libs.bundles.kotlin.inject.compiler)
    implementation(libs.bundles.kotlin.inject)


    // internal dependencies
    implementation(projects.common.networking)
    // can be removed with kotlin-inject-anvil
    implementation(platform((libs.ktor.bom)))
    implementation(libs.bundles.ktor)
}