package sh.kau.playground.landing.di

import androidx.navigation3.runtime.entry
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.settings.nav.SettingsRoutes
import sh.kau.playground.landing.nav.LandingRoutes.LandingScreenRoute
import sh.kau.playground.landing.ui.LandingScreen
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.Navigator
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesSubcomponent(LandingScope::class)
@SingleIn(LandingScope::class)
interface LandingComponent {

    @ContributesSubcomponent.Factory(AppScope::class)
    interface Factory {
        fun create(): LandingComponent

        @Provides
        @IntoSet // kotlin-inject multi-bindings (1)
        fun provideSettingsEntryProvider(
            navigator: Navigator,
            landingComponentFactory: Factory,
            ): EntryProviderInstaller = {
            val settingsComponent = landingComponentFactory.create()
            entry<LandingScreenRoute> {
                LandingScreen {
                    navigator.goTo(SettingsRoutes.ScreenARoute)
                }
            }

        }
    }
}