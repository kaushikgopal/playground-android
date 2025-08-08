package sh.kau.playground.app.di

import android.content.Context
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.landing.nav.LandingRoutes
import sh.kau.playground.features.settings.di.SettingsComponent
import sh.kau.playground.navigation.EntryProviderInstaller
import sh.kau.playground.navigation.NavRoute
import sh.kau.playground.navigation.Navigator
import sh.kau.playground.shared.App
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
) : SettingsComponent.Factory {

  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

  @Provides fun provideDebuggableApp(): @Named("debuggableApp") Boolean = app.isDebuggable

  abstract val loggers: Lazy<Set<LogcatLogger>>

  // region navigation
  @Provides
  @Named("startDestination")
  fun provideStartDestination(): NavRoute = LandingRoutes.LandingScreenRoute

  abstract val navigator: Navigator

  // kotlin-inject multi-bindings (0)
  abstract val entryProviderInstallers: Lazy<Set<EntryProviderInstaller>>

  // endregion

  companion object {

    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      if (instance != null) return instance!!

      instance =
          AppComponent::class.create(
              context.applicationContext as App,
          )

      return instance!!
    }
  }
}
