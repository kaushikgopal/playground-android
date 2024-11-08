import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import kotlin.jvm.kotlin
import kotlin.text.get

class TemplateFeatureConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        plugins.apply(libs.plugins.android.library.get().pluginId)
        plugins.apply(libs.plugins.kotlin.android.get().pluginId)

        dependencies {
          //                add("implementation", libs.findLibrary("javax-inject").get())
          //                add("testImplementation", libs.findLibrary("junit").get())
          //                add("testImplementation", libs.findLibrary("mockk").get())
        }
      }
}
