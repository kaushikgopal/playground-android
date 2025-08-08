package sh.kau.playground.features.settings.viewmodel

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsScope
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(SettingsScope::class, boundType = SettingsAViewModel::class)
@Inject
class SettingsAViewModelImpl(
    coroutineScope: CoroutineScope,
) :
    SettingsAViewModel,
    UsfViewModel<SettingsAEvent, SettingsAUiState, SettingsAEffect>(
        coroutineScope = coroutineScope,
    ) {

  override fun initialState(): SettingsAUiState {
    return SettingsAUiState()
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
