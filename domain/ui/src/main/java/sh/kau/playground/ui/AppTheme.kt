package sh.kau.playground.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Teritiary,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Teritiary,
    secondary = Secondary,
    tertiary = Primary,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )

@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val activity = view.context as Activity
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        activity.window.statusBarColor = colorScheme.background.toArgb()

        // 1. only the navigation bar requires the api check
        //    but as a matter of taste i prefer to keep the colors standard
        //    if i'm unable to change either of them
        // 2. per material 3 design specs, the navigation bar has an elevation of 2
        //    so you'd want to use a color overlay of 8% of the primary color
        //    colorScheme.primary.copy(alpha =
        // 0.08f).compositeOver(colorScheme.surface.copy()).toArgb()
        //    but i prefer having the colors being identical
        activity.window.navigationBarColor = colorScheme.background.toArgb()

        // adjust the icons to match the colors chosen
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars =
          !darkTheme
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars =
          !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
