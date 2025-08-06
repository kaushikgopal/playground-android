package sh.kau.playground.features.landing.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.landing.nav.LandingScreenRoute
import sh.kau.playground.features.landing.ui.LandingScreen
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.features.settings.nav.SettingsRoutes
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(AppScope::class)
interface LandingNavigationModule {
    
    @Provides
    @IntoSet
    fun provideLandingEntryProvider(
        navigator: Navigator,
        @Named("appName") appName: String,
    ): EntryProviderInstaller = {
        entry<LandingScreenRoute> {
            LandingScreen(
                name = appName,
                modifier = Modifier.fillMaxSize(),
                onNavigateToSettings = { 
                    navigator.goTo(SettingsRoutes.ScreenARoute)
                }
            )
        }
    }
}