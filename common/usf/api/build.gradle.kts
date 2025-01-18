plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(platform(libs.coroutines.bom))
    implementation(libs.coroutines.core)

}
