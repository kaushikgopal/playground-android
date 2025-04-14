package sh.kau.playground.app.di

import android.content.Context
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.Named
import sh.kau.playground.features.settings.di.SettingsComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
): SettingsComponent.Factory {

  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

  @Provides fun provideDebuggableApp(): @Named("debuggableApp") Boolean = app.isDebuggable

  abstract val loggers: Set<LogcatLogger> // multi-bindings

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