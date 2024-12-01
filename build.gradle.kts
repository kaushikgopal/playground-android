// Top-level build file where you can add configuration options common to all sub-projects/modules.
// adding it here => you don't have to manually add it in each subproject/build.gradle.kts
//                   also avoids class loader issues when applied in other modules

// apply false    => won't actually apply it to a project (you do that individually)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false // needed for kotlin 2.0

    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.android.lint) apply false
}
