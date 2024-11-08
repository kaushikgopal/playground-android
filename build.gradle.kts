// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Adding it here implies you don't have to manually add it in subproject/build.gradle.kts
// apply false implies we won't actualy apply it to a project - that you can do individually
plugins {
    alias(libs.plugins.android.application) apply false
    // alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false // needed for kotlin 2.0
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
}
