package sh.kau.playground.landing.nav

import kotlinx.serialization.Serializable
import sh.kau.playground.navigation.NavRoute

sealed class LandingRoutes {
  @Serializable data object LandingScreenRoute : NavRoute
}
