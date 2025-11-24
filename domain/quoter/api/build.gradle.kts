plugins {
  id("template.jvm")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies { implementation(libs.kotlinx.serialization.json) }
