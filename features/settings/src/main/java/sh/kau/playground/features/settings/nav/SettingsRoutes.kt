package sh.kau.playground.features.settings.nav

import kotlinx.serialization.Serializable
import sh.kau.playground.navigation.NavRoute

sealed class SettingsRoutes {
  @Serializable data object SettingsGraphRoute

  @Serializable data object ScreenARoute : NavRoute

  @Serializable data object ScreenBRoute : NavRoute
}
//
// fun NavGraphBuilder.addSettingsGraph(
//    settingsComponent: SettingsComponent,
//    navGraphBuilder: NavGraphBuilder,
//    navHostController: NavHostController,
//    modifier:  Modifier, // assisted injection (1)
// ) {
//
//  navGraphBuilder.navigation<SettingsGraphRoute>(startDestination = SettingsRoutes.ScreenARoute) {
//    composable<SettingsRoutes.ScreenARoute> {
//      settingsComponent.settingsAScreen(modifier) {
// navHostController.navigate(SettingsRoutes.ScreenBRoute) }
//    }
//    composable<SettingsRoutes.ScreenBRoute> {
//      // kotlin-inject function injection (3)
//      settingsComponent.settingsBScreen()
//    }
//  }
// }
