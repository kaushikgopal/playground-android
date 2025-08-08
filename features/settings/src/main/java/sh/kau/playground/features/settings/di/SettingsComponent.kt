package sh.kau.playground.features.settings.di

import androidx.navigation3.runtime.entry
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenARoute
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.quoter.api.QuotesRepo
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {

  val settingsBindings: SettingsBindings

  val settingsAScreen: SettingsAScreen
  // kotlin-inject function injection (2)
  val settingsBScreen: SettingsBScreen

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createSettingsComponent(): SettingsComponent

    @Provides
    @IntoSet // kotlin-inject multi-bindings (1)
    fun provideSettingsEntryProvider(settingsComponentFactory: Factory): EntryProviderInstaller = {
      val settingsComponent = settingsComponentFactory.createSettingsComponent()
      entry<ScreenARoute> { settingsComponent.settingsAScreen }

      entry<ScreenBRoute> {
        SettingsBScreen(bindings = settingsComponent.settingsBindings)
        settingsComponent.settingsBScreen
      }
    }
  }
}

@Inject
class SettingsBindings(
    @Named("appName") val appName: String,
    val quotesRepo: QuotesRepo,
)
