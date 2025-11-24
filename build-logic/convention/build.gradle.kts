import org.gradle.accessors.dm.LibrariesForLibs

plugins { `kotlin-dsl` }

group = libs.versions.app.namespace.get() + ".buildlogic"

dependencies {
  compileOnly(libs.gradle.plugin.android)
  compileOnly(libs.gradle.plugin.kotlin)

  // Without the useless cast intelliJ has a syntax highlighting issue and will
  // incorrectly warn that libs.javaClass won't compile
  @Suppress("USELESS_CAST")
  implementation(
      files((libs as LibrariesForLibs).javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {

        register("templateAndroid") {
            // pure basic android module
            id = "template.android"
            implementationClass = "TemplateAndroidConventionPlugin"
        }

        register("templateFeature") {
            // typical UI feature (based on android module above)
            id = "template.feature"
            implementationClass = "TemplateFeatureConventionPlugin"
        }

        register("templateJvm") {
            // pure Kotlin/JVM modules (no Android)
            id = "template.jvm"
            implementationClass = "TemplateJvmConventionPlugin"
        }
    }
}
