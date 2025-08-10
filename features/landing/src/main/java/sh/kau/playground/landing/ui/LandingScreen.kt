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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.nav.SettingsRoutes
import sh.kau.playground.landing.di.LandingScope
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.ui.Primary
import sh.kau.playground.ui.Teritiary
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

typealias LandingScreen = @Composable () -> Unit

@Inject
@SingleIn(LandingScope::class)
@Composable
fun LandingScreen(
    viewModel: LandingViewModel,
    navigator: Navigator,
) {
  val uiState by viewModel.state.collectAsState()

  LaunchedEffect(viewModel) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is LandingEffect.NavigateToSettings -> navigator.goTo(SettingsRoutes.ScreenARoute)
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Primary), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = uiState.subtitle,
          style = MaterialTheme.typography.bodyMedium,
      )
      Text(
          text = uiState.title,
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
          onClick = { viewModel.input(LandingEvent.NavigateToSettingsClicked) },
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
