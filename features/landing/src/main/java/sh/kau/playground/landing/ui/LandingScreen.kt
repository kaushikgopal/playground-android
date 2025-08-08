package sh.kau.playground.landing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import sh.kau.playground.ui.Primary
import sh.kau.playground.ui.Teritiary

@Composable
fun LandingScreen(
//    @Assisted modifier: Modifier,
    onNavigateToSettings: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(Primary), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = "Hello there!",
          style = MaterialTheme.typography.bodyMedium,
//          modifier = modifier,
      )
      Text(
          text = "Landing Screen",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
      )
      OutlinedButton(
          colors =
              ButtonColors(
                  containerColor = Teritiary,
                  contentColor = Color.Black,
                  disabledContainerColor = Primary,
                  disabledContentColor = Color.White,
              ),
          onClick = onNavigateToSettings,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(text = "Go to Settings")
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
//  LandingScreen(name = "Landing Screen") {}
}
