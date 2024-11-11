package sh.kau.features.settings.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import sh.kau.features.settings.ui.SettingsScreen


@Serializable
object SettingsScreenRoute

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsScreenRoute> {
        SettingsScreen()
    }
}