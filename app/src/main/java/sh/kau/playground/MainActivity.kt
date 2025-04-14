package sh.kau.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import sh.kau.playground.app.AppNavHost
import sh.kau.playground.app.di.AppComponent
import sh.kau.playground.domain.ui.PlaygroundTheme

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
          AppNavHost(
            appComponent = appComponent,
            innerPadding = innerPadding,
          )
        }
      }
    }
  }
}
