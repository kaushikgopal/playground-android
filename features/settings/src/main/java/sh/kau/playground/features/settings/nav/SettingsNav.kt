package sh.kau.playground.features.settings.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import sh.kau.playground.features.settings.nav.SettingsRoutes.SettingsGraphRoute
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen

sealed class SettingsRoutes {
  @Serializable data object SettingsGraphRoute

  @Serializable data object ScreenARoute

  @Serializable data object ScreenBRoute
}

fun NavGraphBuilder.addSettingsGraph(
    navGraphBuilder: NavGraphBuilder,
    navHostController: NavHostController,
) {
  navGraphBuilder.navigation<SettingsGraphRoute>(startDestination = SettingsRoutes.ScreenARoute) {
    composable<SettingsRoutes.ScreenARoute> {
      SettingsAScreen { navHostController.navigate(SettingsRoutes.ScreenBRoute) }
    }
    composable<SettingsRoutes.ScreenBRoute> { SettingsBScreen() }
  }
}
