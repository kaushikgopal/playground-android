package sh.kau.playground.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import sh.kau.playground.app.di.AppComponent
import sh.kau.playground.ui.AppTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val appComponent = AppComponent.Companion.from(this)

    enableEdgeToEdge()
    setContent {
      AppTheme {
        val navigator = appComponent.navigator
        val entryProviders = appComponent.entryProviderInstallers

        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
          NavDisplay(
              backStack = navigator.backStack,
              modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding),
              onBack = { 
                  if (!navigator.goBack()) {
                      finish()
                  }
              },
              entryProvider = entryProvider {
                  entryProviders.forEach { installer ->
                      this.installer()
                  }
              }
          )
        }
      }
    }
  }
}
