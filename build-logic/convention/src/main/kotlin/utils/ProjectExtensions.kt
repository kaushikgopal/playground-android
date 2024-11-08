package utils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

// implementation LibrariesForLibs.javaClass.superclass.protectionDomain.codeSource.location
// in build.gradle.kts enables this
val Project.libs
  get() = the<LibrariesForLibs>()
