package sh.kau.playground.features.settings.di

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.features.settings.nav.SettingsRoutes
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.Navigator
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(AppScope::class)
interface SettingsNavigationModule {
    
    @Provides
    @IntoSet
    fun provideSettingsEntryProvider(
        navigator: Navigator,
        settingsComponentFactory: SettingsComponent.Factory
    ): EntryProviderInstaller = {
        entry<SettingsRoutes.ScreenARoute> {
            val settingsComponent = settingsComponentFactory.createSettingsComponent()
            settingsComponent.settingsAScreen(
                Modifier.fillMaxSize(),
                { navigator.goTo(SettingsRoutes.ScreenBRoute) }
            )
        }
        
        entry<SettingsRoutes.ScreenBRoute> {
            val settingsComponent = settingsComponentFactory.createSettingsComponent()
            settingsComponent.settingsBScreen()
        }
    }
}