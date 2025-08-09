package sh.kau.playground.landing.ui

import sh.kau.playground.usf.api.Usf

interface LandingViewModel : Usf<LandingEvent, LandingUiState, LandingEffect>

sealed interface LandingEvent {
  data object NavigateToSettingsClicked : LandingEvent
}

data class LandingUiState(
    val title: String = "Landing Screen",
    val subtitle: String = "Hello there!",
)

sealed interface LandingEffect {
  data object NavigateToSettings : LandingEffect
}
