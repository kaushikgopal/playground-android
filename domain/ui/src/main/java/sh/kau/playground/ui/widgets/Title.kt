package sh.kau.playground.ui.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import sh.kau.playground.ui.AppTypography
import sh.kau.playground.ui.modTopTitlePadding

@Composable
fun Title(text: String) {
  Text(text = "Discover", style = AppTypography.displayMedium, modifier = modTopTitlePadding)
}
