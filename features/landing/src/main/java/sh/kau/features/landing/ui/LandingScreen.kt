package sh.kau.features.landing.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun LandingScreen(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        color = Color.White,
        modifier = modifier
    )
}


//@Preview(showBackground = true)
//@Composable
//fun LandingPreview() {
//    PlaygroundTheme {
//        Scaffold(
//            modifier = Modifier.fillMaxSize(),
//            containerColor = Purple40,
//        ) { innerPadding ->
//            MainScreen(
//                name = "Android Preview",
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
//    }
//}