package sh.kau.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import sh.kau.playground.domain.app.di.AppComponent
import sh.kau.playground.domain.ui.PlaygroundTheme
import sh.kau.playground.features.landing.nav.LandingScreenRoute
import sh.kau.playground.features.landing.nav.addLandingRoute
import sh.kau.playground.features.settings.nav.SettingsRoutes.SettingsGraphRoute
import sh.kau.playground.features.settings.nav.addSettingsGraph

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appComponent = AppComponent.from(this)
    val settingsComponent = appComponent.createSettingsComponent()

    setContent {
      PlaygroundTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
          val navController = rememberNavController()
          NavHost(
              navController = navController,
              startDestination = LandingScreenRoute,
          ) {
            // building the main nav graph
            addLandingRoute(
                modifier = Modifier.padding(innerPadding),
                onNavigateToSettings = { navController.navigate(SettingsGraphRoute) },
            )
            addSettingsGraph(
                settingsComponent = settingsComponent,
                navGraphBuilder = this,
                navHostController = navController,
            )
          }
        }
      }
    }
  }
}
