plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)

    // alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    kotlin("plugin.compose")
}

android {
    namespace = "sh.kau.playground"
    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "sh.kau.playground"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.target.get().toInt()
        versionCode = libs.versions.version.code.get().toInt()
        versionName = libs.versions.version.name.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        // allow kotlin auto-complete in ide
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // kotlin
    implementation(libs.androidx.core.ktx)
    // implementation(platform(libs.kotlin.bom))

    // dependency injection
    ksp(libs.kotlin.inject.compiler)
    implementation(libs.kotlin.inject.runtime)
    ksp(libs.kotlin.inject.anvil.compiler)
    implementation(libs.kotlin.inject.anvil.runtime)
    implementation(libs.kotlin.inject.anvil.runtime.utils)

    // navigation
    implementation(libs.compose.navigation)
    implementation(libs.kotlinx.serialization.json)

    // Compose Bill Of Materials
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.tools.graphics) //
    implementation(libs.compose.tools.preview) // Android Studio Preview support
    implementation(libs.compose.material)
    debugImplementation(libs.compose.tools)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // internal
    implementation(project(":common:log"))
    implementation(project(":domain:shared"))
    implementation(project(":features:landing"))


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    debugImplementation(libs.compose.test.manifest)
}