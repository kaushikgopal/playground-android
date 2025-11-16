package sh.kau.playground.landing.di

import androidx.navigation3.runtime.entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.landing.nav.LandingRoutes.LandingScreenRoute
import sh.kau.playground.landing.ui.LandingScreen
import sh.kau.playground.landing.ui.LandingViewModel
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

  // Explicit @Provides for typealias to work around KSP limitations
  @Provides
  fun provideLandingScreen(
      viewModel: LandingViewModel,
      navigator: Navigator,
  ): LandingScreen = sh.kau.playground.landing.ui.LandingScreen(viewModel, navigator)

  // Expose the screen for navigation
  val landingScreen: LandingScreen

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createLandingComponent(): LandingComponent

    @Provides
    @IntoSet // kotlin-inject multi-bindings (1)
    fun provideSettingsEntryProvider(
        landingComponentFactory: Factory,
    ): EntryProviderInstaller = {
      val component by lazy { landingComponentFactory.createLandingComponent() }
      entry<LandingScreenRoute> { component.landingScreen() }
    }
  }
}
