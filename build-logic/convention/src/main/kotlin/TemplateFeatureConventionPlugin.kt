import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import utils.configExtension
import utils.kotlinOptions
import utils.libs

class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        pluginManager.withPlugin("com.android.application") {
          configExtension<ApplicationExtension> { project.applyAndroidConfig(this) }
        }
        pluginManager.withPlugin("com.android.library") {
          configExtension<LibraryExtension> { project.applyAndroidConfig(this) }
        }
      }

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
      ksp(libs.kotlin.inject.compiler)
      implementation(libs.kotlin.inject.runtime)
      ksp(libs.kotlin.inject.anvil.compiler)
      implementation(libs.kotlin.inject.anvil.runtime)
      implementation(libs.kotlin.inject.anvil.runtime.utils)

      // Compose
      implementation(platform(libs.compose.bom))
      implementation(libs.bundles.compose)

      debugImplementation(libs.compose.tools.preview) // Android Studio Preview support
      // debugImplementation(libs.compose.tools)


      implementation(libs.compose.tools.graphics) //
    }
  }
}
