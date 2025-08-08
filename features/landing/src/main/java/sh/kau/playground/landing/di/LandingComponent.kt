package sh.kau.playground.landing.di

import androidx.navigation3.runtime.entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.settings.nav.SettingsRoutes
import sh.kau.playground.landing.nav.LandingRoutes.LandingScreenRoute
import sh.kau.playground.landing.ui.LandingScreen
import sh.kau.playground.landing.viewmodel.LandingViewModel
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.Navigator
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(LandingScope::class)
@SingleIn(LandingScope::class)
interface LandingComponent {

  @Provides
  @SingleIn(LandingScope::class)
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  fun landingViewModel(): LandingViewModel

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createLandingComponent(): LandingComponent

    @Provides
    @IntoSet // kotlin-inject multi-bindings (1)
    fun provideSettingsEntryProvider(
        navigator: Navigator,
        landingComponentFactory: Factory,
    ): EntryProviderInstaller = {
      entry<LandingScreenRoute> {
        val component = landingComponentFactory.createLandingComponent()
        val viewModel = component.landingViewModel()

        // this is a very manual way of wiring it (more for a demo)
        // use DI more effectively as we do in SettingsComponent
        LandingScreen(
            viewModel = viewModel,
            onNavigateToSettings = { navigator.goTo(SettingsRoutes.ScreenARoute) })
      }
    }
  }
}
