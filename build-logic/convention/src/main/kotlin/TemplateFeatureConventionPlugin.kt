import kotlin.jvm.kotlin
import kotlin.text.get
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.android
import utils.libs

class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        plugins.apply(libs.plugins.android.library.get().pluginId)
        plugins.apply(libs.plugins.kotlin.android.get().pluginId)
        plugins.apply(libs.plugins.kotlin.compose.compiler.get().pluginId)
        plugins.apply(libs.plugins.ksp.get().pluginId)

        android {
          buildFeatures { compose = true }

          packaging {
            resources {
              excludes.add("/META-INF/{AL2.0,LGPL2.1}")
              excludes.add("/META-INF/LICENSE*")
            }
          }
        }

        dependencies {
          val ksp by configurations
          val implementation by configurations

          // dependency injection
          ksp(libs.kotlin.inject.compiler)
          implementation(libs.kotlin.inject.runtime)
          ksp(libs.kotlin.inject.anvil.compiler)
          implementation(libs.kotlin.inject.anvil.runtime)
          implementation(libs.kotlin.inject.anvil.runtime.utils)
        }
      }
}
