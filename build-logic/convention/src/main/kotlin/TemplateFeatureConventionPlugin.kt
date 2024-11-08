import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import utils.libs

class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        plugins.apply(libs.plugins.android.library.get().pluginId)
        plugins.apply(libs.plugins.kotlin.android.get().pluginId)
        // plugins.apply(utils.libs.plugins.ksp.get().pluginId)

        dependencies {
          //                add("implementation", utils.libs.findLibrary("javax-inject").get())
          //                add("testImplementation", utils.libs.findLibrary("junit").get())
          //                add("testImplementation", utils.libs.findLibrary("mockk").get())
        }
      }
}
