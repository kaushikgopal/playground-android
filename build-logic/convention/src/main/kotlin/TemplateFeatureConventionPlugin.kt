import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.configAndroidAppAndLib
import utils.kotlinOptions
import utils.libs

/**
 * You might be inclined to just include a lot of stuff here.
 *
 * Resist that urge! Only truly put things you find are common everywhere.
 */
class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        configAndroidAppAndLib(
            androidApp = { project.applyAndroidConfig(this) },
            androidLib = { project.applyAndroidConfig(this) },
        )
      }

  /** Configurations common to both android app modules & android library modules */
  fun Project.applyAndroidConfig(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
      compileSdk = libs.versions.sdk.compile.get().toInt()
      defaultConfig { minSdk = libs.versions.sdk.min.get().toInt() }
      kotlinOptions {
        // allow kotlin auto-complete in ide
        jvmTarget = "1.8"
      }

      packaging {
        resources {
          excludes.add("/META-INF/{AL2.0,LGPL2.1}")
          excludes.add("/META-INF/LICENSE*")
        }
      }

      buildFeatures { compose = true }
    }

    plugins.apply(libs.plugins.kotlin.android.get().pluginId)
    plugins.apply(libs.plugins.kotlin.compose.compiler.get().pluginId)
    plugins.apply(libs.plugins.ksp.get().pluginId)

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

      debugImplementation(libs.compose.tools.preview) // Android Studio Preview support
      // debugImplementation(libs.compose.tools)

      implementation(libs.compose.tools.graphics) //
    }
  }
}
