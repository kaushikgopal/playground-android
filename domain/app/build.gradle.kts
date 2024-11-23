plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    // internal
    api(projects.domain.ui) // brings in compose
    api(projects.common.networking)

    api(projects.common.log)
}
