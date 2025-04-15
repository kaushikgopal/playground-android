package sh.kau.playground.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import sh.kau.playground.app.AppNavHost
import sh.kau.playground.app.di.AppComponent
import sh.kau.playground.ui.AppTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appComponent = AppComponent.Companion.from(this)

    setContent {
      AppTheme {
        Scaffold(
          modifier = Modifier.Companion.fillMaxSize(),
        ) { innerPadding ->
          // important to pass and use the [innerPadding]
          // as adding top/bottom bar will be accounted for
          // in nested elements (as they will use the right padding value)
          AppNavHost(
            appComponent = appComponent,
            innerPadding = innerPadding,
          )
        }
      }
    }
  }
}
