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
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)

    // Internal
    implementation(project(":common:log"))
    lintChecks(project(":common:lint-rules"))
}