package sh.kau.playground.landing.di

import androidx.navigation3.runtime.entry
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import sh.kau.playground.landing.nav.LandingRoutes.LandingScreenRoute
import sh.kau.playground.landing.ui.LandingScreen
import sh.kau.playground.landing.ui.LandingViewModel
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.shared.di.AppScope

@GraphExtension(LandingScope::class)
interface LandingGraph {

  val landingScreen: LandingScreen

  @Provides
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @Provides
  fun provideLandingScreen(
      viewModel: LandingViewModel,
      navigator: Navigator,
  ): LandingScreen = sh.kau.playground.landing.ui.LandingScreen(viewModel, navigator)

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun createLandingGraph(): LandingGraph

    @Provides
    @IntoSet
    fun provideLandingEntryProvider(
        landingGraphFactory: Factory,
    ): EntryProviderInstaller = {
      val graph by lazy { landingGraphFactory.createLandingGraph() }
      entry<LandingScreenRoute> { graph.landingScreen() }
    }
  }
}
