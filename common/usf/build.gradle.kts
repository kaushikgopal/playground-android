plugins {
    id("template.jvm")
    alias(libs.plugins.java.test.fixtures)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.core)
    testFixturesImplementation(platform(libs.coroutines.bom))

    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testFixturesImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.testing.junit.engine)
    testImplementation(libs.bundles.testing)

//    testImplementation(libs.mockito.core)
//    testImplementation(libs.mockito.kotlin)

}
