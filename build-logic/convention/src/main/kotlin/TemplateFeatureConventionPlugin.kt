import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.android
import utils.libs

/**
 * Feature convention plugin. This is the typical plugin you want to use while building most
 * features.
 *
 * Usage:
 * ```
 * plugins {
 *   id("com.android.library")
 *   id("template.feature")
 * }
 * ```
 *
 * You might be inclined to just include a lot of stuff here. Resist that urge! Only truly put
 * things you find are common everywhere.
 *
 * For android-specific configurations, use the [TemplateAndroidConventionPlugin] instead.
 */
class TemplateFeatureConventionPlugin : TemplateAndroidConventionPlugin() {
  override fun apply(project: Project) {
    with(project) {

      // Bring in the functionality from [TemplateAndroidConventionPlugin]
      super.apply(project)

      android {
        namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"

        buildFeatures { compose = true } // enable compose functionality in Android Studio

        plugins.apply(libs.plugins.kotlin.android.get().pluginId)
        plugins.apply(libs.plugins.kotlin.compose.compiler.get().pluginId)
        plugins.apply(libs.plugins.kotlin.serialization.get().pluginId)

        dependencies {
          // below will pull inner implementation as well
          // be judicious here
          val api by configurations
          api(project(":common:log"))
          api(project(":domain:ui")) // brings in compose
        }
      }
    }
  }
}
