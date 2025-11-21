package sh.kau.playground.features.settings.viewmodel

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import sh.kau.playground.features.settings.di.SettingsScope
import sh.kau.playground.usf.log.UsfLoggingInspector
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel
import sh.kau.playground.usf.viewmodel.inputEventCallback

@ContributesBinding(SettingsScope::class, binding = binding<SettingsAViewModel>())
@Inject
class SettingsAViewModelImpl(
    coroutineScope: CoroutineScope,
) :
    SettingsAViewModel,
    UsfViewModel<SettingsAEvent, SettingsAUiState, SettingsAEffect>(
        coroutineScope = coroutineScope,
        inspector = UsfLoggingInspector("[USF][S-A]"),
    ) {

  override fun initialState(): SettingsAUiState {
    return SettingsAUiState(
        onCheckedChange = inputEventCallback(SettingsAEvent.ToggleChanged),
    )
  }

  override suspend fun ResultScope<SettingsAUiState, SettingsAEffect>.process(
      event: SettingsAEvent
  ) {
    when (event) {
      is SettingsAEvent.BackClicked -> {
        emitEffect(SettingsAEffect.NavigateBack)
      }
      is SettingsAEvent.NavigateToBClicked -> {
        emitEffect(SettingsAEffect.NavigateToSettingsB)
      }
      is SettingsAEvent.ToggleChanged -> {
        updateState { it.copy(toggleEnabled = !it.toggleEnabled) }
      }
    }
  }
}
