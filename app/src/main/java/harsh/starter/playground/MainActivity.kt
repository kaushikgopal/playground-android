package harsh.starter.playground

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
import harsh.starter.playground.di.AppComponent
import harsh.starter.playground.domain.ui.PlaygroundTheme
import harsh.starter.playground.features.landing.nav.LandingScreenRoute
import harsh.starter.playground.features.landing.nav.addLandingRoute
import harsh.starter.playground.features.settings.nav.SettingsRoutes.SettingsGraphRoute
import harsh.starter.playground.features.settings.nav.addSettingsGraph

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appComponent = AppComponent.from(this)

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
                settingsComponent = appComponent.createSettingsComponent(),
                navGraphBuilder = this,
                navHostController = navController,
            )
          }
        }
      }
    }
  }
}
