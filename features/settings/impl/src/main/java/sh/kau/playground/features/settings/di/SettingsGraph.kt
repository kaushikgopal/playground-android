package sh.kau.playground.features.settings.di

import androidx.navigation3.runtime.entry
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenARoute
import sh.kau.playground.features.settings.nav.SettingsRoutes.ScreenBRoute
import sh.kau.playground.features.settings.ui.SettingsAScreen
import sh.kau.playground.features.settings.ui.SettingsBScreen
import sh.kau.playground.features.settings.viewmodel.SettingsBViewModel
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.shared.di.AppScope

@GraphExtension(SettingsScope::class)
interface SettingsGraph {

  val settingsAScreen: SettingsAScreen
  val settingsBScreen: SettingsBScreen

  @Provides
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  // Explicit @Provides for typealias to work around @Inject limitations on top-level functions.
  @Provides
  fun provideSettingsBScreen(viewModel: SettingsBViewModel): SettingsBScreen =
      SettingsBScreen(viewModel)

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun createSettingsGraph(): SettingsGraph

    @Provides
    @IntoSet
    fun provideSettingsEntryProvider(factory: Factory): EntryProviderInstaller = {
      val settingsGraph by lazy { factory.createSettingsGraph() }
      entry<ScreenARoute> { settingsGraph.settingsAScreen() }
      entry<ScreenBRoute> { settingsGraph.settingsBScreen() }
    }
  }
}

@Inject
class SettingsBindings(
    @param:Named("appName") val appName: String,
)
