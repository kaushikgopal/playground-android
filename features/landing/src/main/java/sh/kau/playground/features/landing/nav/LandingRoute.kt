package sh.kau.playground.features.landing.nav

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import sh.kau.playground.features.landing.ui.LandingScreen
import sh.kau.playground.navigation.NavRoute

@Serializable object LandingScreenRoute : NavRoute

fun NavGraphBuilder.addLandingRoute(
    name: String = "Default name",
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
) {
  composable<LandingScreenRoute> {
    LandingScreen(
        name = name,
        modifier = modifier,
        onNavigateToSettings = onNavigateToSettings,
    )
  }
}
