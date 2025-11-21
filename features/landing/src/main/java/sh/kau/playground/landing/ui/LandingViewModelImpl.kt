package sh.kau.playground.landing.ui

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import sh.kau.playground.landing.di.LandingScope
import sh.kau.playground.usf.log.UsfLoggingInspector
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel

@ContributesBinding(LandingScope::class, binding = binding<LandingViewModel>())
@Inject
class LandingViewModelImpl(
    coroutineScope: CoroutineScope,
) :
    LandingViewModel,
    UsfViewModel<LandingEvent, LandingUiState, LandingEffect>(
        coroutineScope = coroutineScope,
        inspector = UsfLoggingInspector("[USF][LVM]"),
    ) {

  override fun initialState(): LandingUiState = LandingUiState()

  override suspend fun ResultScope<LandingUiState, LandingEffect>.process(event: LandingEvent) {
    when (event) {
      is LandingEvent.NavigateToSettingsClicked -> {
        emitEffect(LandingEffect.NavigateToSettings)
      }
    }
  }
}
