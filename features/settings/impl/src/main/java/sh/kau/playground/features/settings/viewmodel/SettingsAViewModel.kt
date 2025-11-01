package sh.kau.playground.features.settings.viewmodel

import sh.kau.playground.usf.api.Usf

interface SettingsAViewModel : Usf<SettingsAEvent, SettingsAUiState, SettingsAEffect>

sealed interface SettingsAEvent {
  data object BackClicked : SettingsAEvent

  data object NavigateToBClicked : SettingsAEvent

  data object ToggleChanged : SettingsAEvent
}

data class SettingsAUiState(
    val title: String = "Settings A",
    val toggleEnabled: Boolean = false,
    val onCheckedChange: () -> Unit,
)

sealed interface SettingsAEffect {
  data object NavigateBack : SettingsAEffect

  data object NavigateToSettingsB : SettingsAEffect
}
