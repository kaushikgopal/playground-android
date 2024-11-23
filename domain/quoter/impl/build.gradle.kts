plugins {
    id("com.android.library")
    id("template.android")
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"
}

dependencies {
//    api(platform(":domain:quoter:api"))

    // internal dependencies
//    implementation(platform(":common:networking"))
    implementation(projects.common.networking)
}