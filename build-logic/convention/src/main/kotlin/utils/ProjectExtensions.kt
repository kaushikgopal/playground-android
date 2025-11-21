package utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

// implementation LibrariesForLibs.javaClass.superclass.protectionDomain.codeSource.location
// in build.gradle.kts enables this
val Project.libs
  get() = the<LibrariesForLibs>()

inline fun Project.kotlin(block: KotlinProjectExtension.() -> Unit): KotlinProjectExtension {
  return project.extensions.getByType(KotlinProjectExtension::class.java).apply(block)
}

inline fun Project.configAndroidAppAndLib(
    crossinline androidApp: ApplicationExtension.() -> Unit,
    crossinline androidLib: LibraryExtension.() -> Unit,
) {
  pluginManager.withPlugin("com.android.library") { android { androidLib.invoke(this) } }
  pluginManager.withPlugin("com.android.application") { app { androidApp.invoke(this) } }
}

inline fun Project.app(block: ApplicationExtension.() -> Unit): ApplicationExtension {
  return project.extensions.getByType(ApplicationExtension::class.java).apply(block)
}

inline fun Project.android(block: LibraryExtension.() -> Unit): LibraryExtension {
  return project.extensions.getByType(LibraryExtension::class.java).apply(block)
}

inline fun <reified T : Any> Project.configExtension(crossinline configure: T.() -> Unit) {
  extensions.findByType(T::class)?.configure()
}
