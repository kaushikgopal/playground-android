package sh.kau.playground.features.settings.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.features.settings.di.SettingsScope
import sh.kau.playground.quoter.Quote
import sh.kau.playground.quoter.QuotesRepo
import sh.kau.playground.usf.log.UsfLoggingInspector
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(SettingsScope::class, boundType = SettingsBViewModel::class)
class SettingsBViewModelImpl
@Inject
constructor(
    coroutineScope: CoroutineScope,
    val quotesRepo: Lazy<QuotesRepo>,
) :
    UsfViewModel<SettingsBEvent, SettingsBUiState, SettingsBEffect>(
        coroutineScope = coroutineScope,
        inspector = UsfLoggingInspector("[USF][S-B]"),
    ),
    SettingsBViewModel {

  val defaultQuote = Quote("Get to the CHOPPER!!!", "Arnold Schwarzenegger")

  override fun initialState(): SettingsBUiState {
    return SettingsBUiState(
        quoteText = defaultQuote.quote,
        quoteAuthor = defaultQuote.author,
    )
  }

  override fun ResultScope<SettingsBUiState, SettingsBEffect>.onSubscribed() {
    // Load quote for the day
    coroutineScope.launch(Dispatchers.IO) {
      val quote = quotesRepo.value.quoteForTheDay()
      updateState { currentState ->
        currentState.copy(
            quoteText = quote.quote,
            quoteAuthor = quote.author,
        )
      }
    }
  }

  override suspend fun ResultScope<SettingsBUiState, SettingsBEffect>.process(
      event: SettingsBEvent
  ) {}
}
