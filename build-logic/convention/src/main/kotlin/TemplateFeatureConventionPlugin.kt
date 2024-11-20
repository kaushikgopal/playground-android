import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.configAndroidAppAndLib
import utils.kotlinOptions
import utils.libs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * You might be inclined to just include a lot of stuff here.
 *
 * Resist that urge! Only truly put things you find are common everywhere.
 */
class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        configAndroidAppAndLib(
          androidApp = {
            project.applyAndroidConfig(this)

            // app level lint settings
            lint {
              quiet = true
              // if true, stop the gradle build if errors are found
              abortOnError = true
              // if true, only report errors
              ignoreWarnings = true
              // Produce report for CI:
              // https://docs.github.com/en/github/finding-security-vulnerabilities-and-errors-in-your-code/sarif-support-for-code-scanning
              // sarifOutput = file("../lint-results.sarif")
              textReport = true
            }
          },
          androidLib = {
            project.applyAndroidConfig(this)
          },
        )
      }

  /** Configurations common to both android app modules & android library modules */
  private fun Project.applyAndroidConfig(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
      compileSdk = libs.versions.sdk.compile.get().toInt()
      defaultConfig { minSdk = libs.versions.sdk.min.get().toInt() }
      kotlinOptions {
        // allow kotlin auto-complete in ide
        jvmTarget.set(JvmTarget.JVM_1_8)
      }

      packaging {
        resources {
          excludes.add("/META-INF/{AL2.0,LGPL2.1}")
          excludes.add("/META-INF/LICENSE*")
        }
      }

      buildFeatures { compose = true } // enable compose functionality in Android Studio

      lint {
        // run lint on dependencies of this project
        checkDependencies = true
      }
    }

    plugins.apply(libs.plugins.kotlin.android.get().pluginId)
    plugins.apply(libs.plugins.kotlin.compose.compiler.get().pluginId)
    plugins.apply(libs.plugins.ksp.get().pluginId)
    plugins.apply(libs.plugins.kotlin.serialization.get().pluginId)

    dependencies {
      val ksp by configurations
      val implementation by configurations
      val debugImplementation by configurations

      // dependency injection
      ksp(libs.bundles.kotlin.inject.compiler)
      implementation(libs.bundles.kotlin.inject)

      // Compose
      implementation(platform(libs.compose.bom))
      implementation(libs.bundles.compose)
      // AndroidStudio Preview support
      implementation(libs.compose.tooling.preview)
      debugImplementation(libs.compose.tooling)

      // Navigation
      implementation(libs.compose.navigation)
      implementation(libs.kotlinx.serialization.json)

      // internal dependencies
      // be very judicious in adding more dependencies here
      implementation(project(":common:log"))
      implementation(project(":domain:ui"))

      // enable lint
      val lintChecks by configurations
      lintChecks(project(":common:lint-rules"))

      //  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    }
  }
}
