plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies { implementation(libs.kotlinx.serialization.json) }
