package sh.kau.playground.landing.di

import androidx.navigation3.runtime.entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.landing.nav.LandingRoutes.LandingScreenRoute
import sh.kau.playground.landing.ui.LandingScreen
import sh.kau.playground.navigation.EntryProviderInstaller
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(LandingScope::class)
@SingleIn(LandingScope::class)
interface LandingComponent {

  val landingScreen: Lazy<LandingScreen>

  @Provides
  @SingleIn(LandingScope::class)
  fun provideCoroutineScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun createLandingComponent(): LandingComponent

    @Provides
    @IntoSet // kotlin-inject multi-bindings (1)
    fun provideSettingsEntryProvider(
        landingComponentFactory: Factory,
    ): EntryProviderInstaller = {
      entry<LandingScreenRoute> {
        val component by lazy { landingComponentFactory.createLandingComponent() }
        component.landingScreen.value()
      }
    }
  }
}
