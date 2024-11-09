package sh.kau.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import sh.kau.features.landing.ui.LandingScreen
import sh.kau.playground.ui.PlaygroundTheme
import sh.kau.playground.ui.Purple40

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PlaygroundTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Purple40,
        ) { innerPadding ->
          LandingScreen(name = "Android", modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}
