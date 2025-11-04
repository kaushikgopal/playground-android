plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.parent?.name}.${project.parent?.name}.${project.name}"
}

dependencies {
    // Android logcat for runtime logging
    api(libs.logcat)

    // USF core interfaces
    implementation(projects.common.usf)
}

