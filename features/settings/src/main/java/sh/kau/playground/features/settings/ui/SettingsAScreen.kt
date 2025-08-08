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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import logcat.logcat
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsBindings
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.ui.Teritiary

typealias SettingsAScreen = @Composable (modifier: Modifier) -> Unit // assisted injection (2)

@Inject
@Composable
fun SettingsAScreen(
    bindings: SettingsBindings,
    navigator: Navigator,
    // example of using kotlin-inject assisted injection
    // when you need to pass something from the main module "in" to this otherwise injected class
    @Assisted modifier: Modifier, // assisted injection (3)
) {
  logcat("SettingsA") { "xxx injected app name →  ${bindings.appName}" }

  Box(
      modifier = modifier.fillMaxSize().background(Teritiary),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Settings A Screen",
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
          )
          Button(
              // TODO: with Nav3 we can just do this in VM
              onClick = { navigator.goTo(ScreenBRoute) },
              modifier = Modifier.align(Alignment.CenterHorizontally),
          ) {
            Text(text = "Settings B")
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
fun SettingsAScreenPreview() {
  //  SettingsAScreen() {}
}
