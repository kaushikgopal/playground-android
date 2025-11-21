package sh.kau.playground.app.di

import android.content.Context
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import logcat.LogcatLogger
import sh.kau.playground.features.settings.di.SettingsGraph
import sh.kau.playground.landing.di.LandingGraph
import sh.kau.playground.landing.nav.LandingRoutes
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.NavRoute
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.shared.App
import sh.kau.playground.shared.di.AppScope

@AppScope
@DependencyGraph(AppScope::class)
interface AppGraph : LandingGraph.Factory, SettingsGraph.Factory {

  @Named("appName") val appName: String
  val navigator: Navigator
  val loggers: Lazy<Set<LogcatLogger>>
  val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>

  @Provides @Named("appName") fun provideAppName(): String = "My Playground!"

  @Provides @Named("debuggableApp") fun provideDebuggableApp(app: App): Boolean = app.isDebuggable

  @Provides
  @Named("startDestination")
  fun provideStartDestination(): NavRoute = LandingRoutes.LandingScreenRoute

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides app: App): AppGraph
  }
}

fun createAppGraph(context: Context): AppGraph =
    createGraphFactory<AppGraph.Factory>().create(context.applicationContext as App)
