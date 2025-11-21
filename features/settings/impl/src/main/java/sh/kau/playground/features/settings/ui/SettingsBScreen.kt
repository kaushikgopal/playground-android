package sh.kau.playground.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import sh.kau.playground.features.settings.viewmodel.SettingsBViewModel
import sh.kau.playground.ui.Secondary
import sh.kau.playground.ui.dp16
import sh.kau.playground.ui.dp24
import sh.kau.playground.ui.dp32

// Metro function injection (2)
typealias SettingsBScreen = @Composable () -> Unit

// Metro function injection (1)
// Note: @Inject on top-level functions doesn't work with typealiases,
// so Landing/Settings graphs expose these via @Provides functions instead.
fun SettingsBScreen(
    viewModel: SettingsBViewModel,
): SettingsBScreen = {
  val uiState by viewModel.state.collectAsState()

  Box(
      modifier = Modifier.fillMaxSize().background(Secondary),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dp16),
        modifier = Modifier.padding(dp16),
    ) {
      Text(
          text = uiState.title,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = dp24),
      )

      // Display a default quote
      Text(
          text = uiState.quoteText,
          style = MaterialTheme.typography.bodyLarge,
          fontStyle = FontStyle.Italic,
          modifier = Modifier.padding(horizontal = dp32, vertical = dp16),
          textAlign = TextAlign.Center,
      )

      Text(
          text = "- ${uiState.quoteAuthor}",
          style = MaterialTheme.typography.bodyMedium,
          fontStyle = FontStyle.Italic,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsBScreenPreview() {
  //  val quotesRepoImpl =
  //      object : QuotesRepo {
  //        override fun quoteForTheDay(): Quote {
  //          return Quote("Get to the CHOPPER!!!", "Arnold Schwarzenegger")
  //        }
  //      }
  //  val bindings = SettingsBindings("Playground", quotesRepoImpl)
  //  SettingsBScreen(bindings)
}
