package sh.kau.playground.features.settings.di

import androidx.navigation3.runtime.entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenARoute
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(SettingsScope::class)
@SingleIn(SettingsScope::class)
interface SettingsComponent {

  val settingsAScreen: Lazy<SettingsAScreen>
  // kotlin-inject function injection (3)
  val settingsBScreen: Lazy<SettingsBScreen>

  @Provides
  @SingleIn(SettingsScope::class)
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createSettingsComponent(): SettingsComponent

    @Provides
    @IntoSet
    fun provideSettingsEntryProvider(factory: Factory): EntryProviderInstaller = {
      // create component on first navigation
      // remember factory here is the AppComponent itself (it implements the interface)
      val settingsComponent by lazy { factory.createSettingsComponent() }
      entry<ScreenARoute> { settingsComponent.settingsAScreen.value() }
      entry<ScreenBRoute> { settingsComponent.settingsBScreen.value() }
    }
  }
}

@Inject
@SingleIn(SettingsScope::class)
class SettingsBindings(
    @Named("appName") val appName: String,
)
