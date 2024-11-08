package sh.kau.features.landing.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LandingScreen(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", color = Color.White, modifier = modifier)
}

// @Preview(showBackground = true)
// @Composable
// fun LandingScreenPreview() {
//    PlaygroundTheme {
//        Scaffold(
//            modifier = Modifier.fillMaxSize(),
//            containerColor = Purple40,
//        ) { innerPadding ->
//            LandingScreen(
//                name = "Android Preview",
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
//    }
// }
