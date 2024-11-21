import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import utils.configAndroidAppAndLib
import utils.kotlinOptions
import utils.libs

/**
 * Base convention plugin for Android modules that provides common Android configurations.
 *
 * Usage:
 * ```
 * plugins {
 *   id("com.android.library")  // or id("com.android.application")
 *   id("template.android")
 * }
 * ```
 *
 * This plugin provides:
 * - Basic Android configuration (compileSdk, minSdk, etc)
 * - Kotlin configuration with JVM target 1.8
 * - Common dependencies:
 *     - Kotlin standard library
 *     - Core Android dependencies
 *     - Test dependencies
 *
 * You might be inclined to just include a lot of stuff here. Resist that urge! Only truly put
 * things you find are common everywhere.
 *
 * For feature-specific configurations, use the [TemplateFeatureConventionPlugin] instead.
 */
open class TemplateAndroidConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      configAndroidAppAndLib(
          androidApp = { project.applyAndroidConfig(this) },
          androidLib = { project.applyAndroidConfig(this) },
      )
    }
  }

  /** Configurations common to both android app modules & android library modules */
  private fun Project.applyAndroidConfig(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    plugins.apply(libs.plugins.kotlin.android.get().pluginId)
    plugins.apply(libs.plugins.ksp.get().pluginId)

    commonExtension.apply {
      compileSdk = libs.versions.sdk.compile.get().toInt()
      defaultConfig { minSdk = libs.versions.sdk.min.get().toInt() }

      kotlinOptions {
        // allow kotlin auto-complete + prevent weird compilation errors in IDE
        jvmTarget.set(JvmTarget.JVM_1_8)
      }

      packaging {
        resources {
          excludes.add("/META-INF/{AL2.0,LGPL2.1}")
          excludes.add("/META-INF/LICENSE*")
        }
      }

      lint {
        // run lint on dependencies of this project
        checkDependencies = true
      }
    }

    dependencies {
      val implementation by configurations
      val ksp by configurations

      // Navigation
      implementation(libs.compose.navigation)
      implementation(libs.kotlinx.serialization.json)

      // dependency injection
      ksp(libs.bundles.kotlin.inject.compiler)
      implementation(libs.bundles.kotlin.inject)

      // internal dependencies
      // be very judicious in adding more dependencies here

      // enable lint
      val lintChecks by configurations
      lintChecks(project(":common:lint-rules"))

      //  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    }
  }
}
