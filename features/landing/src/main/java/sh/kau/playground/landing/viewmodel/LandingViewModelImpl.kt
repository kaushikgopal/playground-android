package sh.kau.playground.landing.viewmodel

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.landing.di.LandingScope
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(LandingScope::class, boundType = LandingViewModel::class)
@Inject
class LandingViewModelImpl(
    coroutineScope: CoroutineScope,
) :
    LandingViewModel,
    UsfViewModel<LandingEvent, LandingUiState, LandingEffect>(
        coroutineScope = coroutineScope,
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
