package sh.kau.playground.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import sh.kau.playground.app.di.AppComponent
import sh.kau.playground.features.landing.nav.LandingScreenRoute
import sh.kau.playground.features.landing.nav.addLandingRoute
import sh.kau.playground.features.settings.nav.SettingsRoutes.SettingsGraphRoute
import sh.kau.playground.features.settings.nav.addSettingsGraph

@Composable
fun ComponentActivity.AppNavHost(
    appComponent: AppComponent,
    innerPadding: PaddingValues,
) {
  // building the main nav graph
  val navController = rememberNavController()
  NavHost(
      navController = navController,
      startDestination = LandingScreenRoute, // starting screen
  ) {
    addLandingRoute(
        modifier = Modifier.padding(innerPadding),
        onNavigateToSettings = { navController.navigate(SettingsGraphRoute) },
    )
    addSettingsGraph(
        settingsComponent = appComponent.createSettingsComponent(),
        navGraphBuilder = this,
        navHostController = navController,
    )
  }
}
