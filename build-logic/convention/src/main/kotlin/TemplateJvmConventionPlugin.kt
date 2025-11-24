import org.gradle.api.Plugin
import org.gradle.api.Project
import utils.kotlin
import utils.libs

/**
 * Convention plugin for non-Android Kotlin/JVM modules.
 *
 * Applies the Kotlin JVM plugin and enforces the Java 17 toolchain so compiled classfiles remain
 * consistent regardless of the developer's host JDK.
 */
class TemplateJvmConventionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      plugins.apply(libs.plugins.kotlin.jvm.get().pluginId)
      kotlin { jvmToolchain(17) }
    }
  }
}

