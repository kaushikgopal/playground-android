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
import sh.kau.features.landing.nav.LandingScreenRoute
import sh.kau.features.landing.nav.landingScreen
import sh.kau.features.settings.nav.SettingsScreenRoute
import sh.kau.features.settings.nav.settingsScreen
import sh.kau.playground.ui.PlaygroundTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

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
            // building the main nava graph
            landingScreen(
                modifier = Modifier.padding(innerPadding),
                onNavigateToSettings = { navController.navigate(SettingsScreenRoute) },
            )
            settingsScreen()
          }
        }
      }
    }
  }
}
