import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.configAndroidAppAndLib
import utils.kotlin
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

    // Use JVM Toolchain for consistent Java/Kotlin compilation
    kotlin { jvmToolchain(17) }

    commonExtension.apply {
      compileSdk = libs.versions.sdk.compile.get().toInt()
      defaultConfig { minSdk = libs.versions.sdk.min.get().toInt() }

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

      // --- project dependencies
      // Be super super judicious about what you put here
      // more often than not, it's easier to just declare the dependencies manually again
      // You won't gain much by "reuse"
      // be very judicious in adding more dependencies here
      implementation(project(":domain:shared"))

      // enable lint
      val lintChecks by configurations
      lintChecks(project(":common:lint-rules"))

      // implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    }
  }
}
