plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"

    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/LICENSE*")
        }
    }
}

dependencies {

    // Compose
    // using api (so modules that depend on this one can leverage compose directly)
    api(platform(libs.compose.bom))
    api(libs.bundles.compose)
    // AndroidStudio Preview support
    api(libs.compose.tooling.preview)
    debugApi(libs.compose.tooling)

    // Internal
    implementation(project(":common:log"))
    lintChecks(project(":common:lint-rules"))
}