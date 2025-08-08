package sh.kau.playground.features.settings.nav

import kotlinx.serialization.Serializable
import sh.kau.playground.navigation.NavRoute

sealed class SettingsRoutes {
  @Serializable data object ScreenARoute : NavRoute

  @Serializable data object ScreenBRoute : NavRoute
}
