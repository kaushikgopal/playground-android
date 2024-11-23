plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
    api(projects.domain.quoter.api)
    // internal dependencies
    implementation(projects.common.networking)
}