package utils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import com.android.build.gradle.LibraryExtension
import org.gradle.kotlin.dsl.findByType

// implementation LibrariesForLibs.javaClass.superclass.protectionDomain.codeSource.location
// in build.gradle.kts enables this
val Project.libs
  get() = the<LibrariesForLibs>()

inline fun Project.android(block: LibraryExtension.() -> Unit): LibraryExtension {
  return project.extensions.getByType(LibraryExtension::class.java).apply(block)
}

inline fun <reified T : Any> Project.configExtension(crossinline configure: T.() -> Unit) {
  extensions.findByType(T::class)?.configure()
}
