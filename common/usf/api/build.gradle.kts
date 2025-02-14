plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.java.test.fixtures)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.core)

    testFixturesImplementation(platform(libs.coroutines.bom))
    testFixturesImplementation(libs.bundles.testing)

    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testRuntimeOnly(libs.testing.junit.engine)
    testImplementation(libs.bundles.testing)
    testImplementation(testFixtures(projects.common.usf.api))

//    testImplementation(libs.mockito.core)
//    testImplementation(libs.mockito.kotlin)

}
