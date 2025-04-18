package sh.kau.playground.features.settings.nav

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import sh.kau.playground.features.settings.di.SettingsComponent
import sh.kau.playground.features.settings.nav.SettingsRoutes.SettingsGraphRoute

sealed class SettingsRoutes {
  @Serializable data object SettingsGraphRoute

  @Serializable data object ScreenARoute

  @Serializable data object ScreenBRoute
}

fun NavGraphBuilder.addSettingsGraph(
    settingsComponent: SettingsComponent,
    navGraphBuilder: NavGraphBuilder,
    navHostController: NavHostController,
    modifier:  Modifier, // assisted injection (1)
) {

  navGraphBuilder.navigation<SettingsGraphRoute>(startDestination = SettingsRoutes.ScreenARoute) {
    composable<SettingsRoutes.ScreenARoute> {
      settingsComponent.settingsAScreen(modifier) { navHostController.navigate(SettingsRoutes.ScreenBRoute) }
    }
    composable<SettingsRoutes.ScreenBRoute> {
      // kotlin-inject function injection (3)
      settingsComponent.settingsBScreen()
    }
  }
}
