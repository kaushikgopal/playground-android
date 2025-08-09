package sh.kau.playground.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import logcat.logcat
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsBindings
import sh.kau.playground.features.settings.di.SettingsScope
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.features.settings.viewmodel.SettingsAEffect
import sh.kau.playground.features.settings.viewmodel.SettingsAEvent
import sh.kau.playground.features.settings.viewmodel.SettingsAViewModel
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.ui.Teritiary
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(SettingsScope::class)
class SettingsAScreen(
    private val bindings: SettingsBindings,
    private val navigator: Navigator,
    private val viewModel: SettingsAViewModel,
) {
  @Composable
  fun Content() {
    logcat("SettingsA") { "xxx injected app name →  ${bindings.appName}" }

    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is SettingsAEffect.NavigateBack -> navigator.goBack()
          is SettingsAEffect.NavigateToSettingsB -> navigator.goTo(ScreenBRoute)
        }
      }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Teritiary),
        contentAlignment = Alignment.Center) {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
              modifier = Modifier.padding(16.dp)) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text("Enable Feature", style = MaterialTheme.typography.bodyLarge)
                      Switch(
                          checked = uiState.toggleEnabled,
                          onCheckedChange = { viewModel.input(SettingsAEvent.ToggleChanged) },
                      )
                    }

                Button(
                    onClick = { viewModel.input(SettingsAEvent.NavigateToBClicked) },
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
