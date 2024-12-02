package harsh.starter.playground.features.settings.di

import me.tatarka.inject.annotations.Inject
import harsh.starter.playground.domain.quoter.api.QuotesRepo
import harsh.starter.playground.domain.shared.di.Named
import harsh.starter.playground.features.settings.ui.SettingsAScreen
import harsh.starter.playground.features.settings.ui.SettingsBScreen
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {

  val settingsAScreen: SettingsAScreen

  // kotlin-inject function injection (2)
  val settingsBScreen: SettingsBScreen

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createSettingsComponent(): SettingsComponent
  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
    val quotesRepo: QuotesRepo,
)
