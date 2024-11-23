plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // internal
    api(project(":common:log"))
    api(project(":domain:ui")) // brings in compose
    api(project(":common:networking"))
}
