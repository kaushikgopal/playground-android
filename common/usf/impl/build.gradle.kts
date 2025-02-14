plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // dependency injection
//    ksp(libs.bundles.kotlin.inject.compiler)
//    implementation(libs.bundles.kotlin.inject)
    // networking implementation
//    implementation(platform((libs.ktor.bom)))
//    api(libs.bundles.ktor) // need ktor classes (outside of NetworkApi)
    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.core)

    testRuntimeOnly(libs.testing.junit.engine)
    testImplementation(libs.bundles.testing)
    testImplementation(testFixtures(projects.common.usf.api))

    // --- project dependencies
    api(projects.common.usf.api)
    implementation(projects.common.log)
    testImplementation(projects.common.usf.api)

}
