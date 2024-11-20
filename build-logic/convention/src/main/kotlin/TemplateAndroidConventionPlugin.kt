import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import utils.android
import utils.kotlinOptions
import utils.libs

/**
 * You might be inclined to just include a lot of stuff here.
 *
 * Resist that urge! Only truly put things you find are common everywhere.
 */
class TemplateAndroidConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        plugins.apply(libs.plugins.android.library.get().pluginId)
        plugins.apply(libs.plugins.kotlin.android.get().pluginId)

        android {
          namespace = libs.versions.app.namespace.get() + ".${project.parent?.name}.${project.name}"

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
          // val implementation by configurations
          // implementation(platform(libs.compose.bom))

          val lintChecks by configurations
          lintChecks(project(":common:lint-rules"))
        }
      }
}
