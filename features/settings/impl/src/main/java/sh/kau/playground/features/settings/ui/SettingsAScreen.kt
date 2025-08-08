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
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsBindings
import sh.kau.playground.features.settings.di.SettingsScope
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.ui.Teritiary
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(SettingsScope::class)
class SettingsAScreen(
  private val bindings: SettingsBindings,
  private val navigator: Navigator,
) {
  @Composable
  fun Content() {
    logcat("SettingsA") { "xxx injected app name →  ${bindings.appName}" }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Teritiary),
      contentAlignment = Alignment.Center
    ) {
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
}

@Preview(showBackground = true)
@Composable
fun SettingsAScreenPreview() {
  //  SettingsAScreen() {}
}
