package sh.kau.playground.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import sh.kau.playground.domain.ui.Pink40

@Composable
fun SettingsAScreen(
    onNavigateToSettingsB: () -> Unit,
) {

  Box(modifier = Modifier.fillMaxSize().background(Pink40), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = "Settings A Screen",
          color = Color.White,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
      )
      Button(
          onClick = onNavigateToSettingsB,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(text = "Settings B", color = Color.White)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsAScreenPreview() {
  SettingsAScreen() {}
}
