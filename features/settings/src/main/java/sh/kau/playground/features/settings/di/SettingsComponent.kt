package sh.kau.playground.features.settings.di

import me.tatarka.inject.annotations.Inject
import sh.kau.playground.quoter.api.QuotesRepo
import sh.kau.playground.shared.di.Named
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen
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
