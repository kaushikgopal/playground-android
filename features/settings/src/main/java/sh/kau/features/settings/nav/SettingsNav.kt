package sh.kau.features.settings.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import sh.kau.features.settings.nav.SettingsRoutes.ScreenARoute
import sh.kau.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.features.settings.nav.SettingsRoutes.SettingsGraphRoute
import sh.kau.features.settings.ui.SettingsAScreen
import sh.kau.features.settings.ui.SettingsBScreen

sealed class SettingsRoutes {
  @Serializable data object SettingsGraphRoute

  @Serializable data object ScreenARoute

  @Serializable data object ScreenBRoute
}

fun NavGraphBuilder.addSettingsGraph(
    navGraphBuilder: NavGraphBuilder,
    navHostController: NavHostController,
) {
  navGraphBuilder.navigation<SettingsGraphRoute>(startDestination = ScreenARoute) {
    composable<ScreenARoute> { SettingsAScreen { navHostController.navigate(ScreenBRoute) } }
    composable<ScreenBRoute> { SettingsBScreen() }
  }
}
