package sh.kau.playground.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsBindings
import sh.kau.playground.quoter.api.Quote
import sh.kau.playground.ui.Secondary

// kotlin-inject function injection (1)
typealias SettingsBScreen = @Composable () -> Unit

@Inject
@Composable
fun SettingsBScreen(bindings: SettingsBindings) {

  // TODO: use USF like pattern
  var quote by remember { mutableStateOf<Quote?>(null) }
  LaunchedEffect(Unit) { quote = bindings.quotesRepo.quoteForTheDay() }

  Box(
      modifier = Modifier.fillMaxSize().background(Secondary),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Settings B Screen",
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 24.dp),
          )

          if (quote == null) {
            quote = Quote("Get to the CHOPPER!!!", "Arnold Schwarzenegger")
          }

          Text(
              text = "\"${quote!!.quote}\"",
              style = MaterialTheme.typography.bodyLarge,
              fontStyle = FontStyle.Italic,
              modifier = Modifier.padding(horizontal = 32.dp),
              textAlign = TextAlign.Center,
          )

          Text(
              text = "- ${quote!!.author}",
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
