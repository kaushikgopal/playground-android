plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // dependencies you want most other feature modules to have
    // be judicious
    api(libs.logcat)

    // --- project dependencies
    implementation(projects.common.usf) // for usf logging alone
}
