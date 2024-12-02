package harsh.starter.playground.features.landing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import harsh.starter.playground.domain.ui.Purple40

@Composable
fun LandingScreen(
    name: String,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(Purple40), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = "Hello $name!",
          color = Color.White,
          style = MaterialTheme.typography.bodyMedium,
          modifier = modifier)
      Text(
          text = "Landing Screen",
          color = Color.White,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold)
      OutlinedButton(
          onClick = onNavigateToSettings,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(text = "Go to Settings", color = Color.White)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
  LandingScreen(name = "Landing Screen") {}
}
