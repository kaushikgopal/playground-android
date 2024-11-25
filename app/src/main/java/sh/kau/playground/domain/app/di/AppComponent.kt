package sh.kau.playground.domain.app.di

import android.content.Context
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.domain.shared.App
import sh.kau.playground.domain.shared.di.Named
import sh.kau.playground.features.settings.di.SettingsComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent(
    @get:Provides val app: App,
    // component inheritance ↓
    // dependencies from below will now be available to AppComponent
//    @Component val logComponent: LogComponent,
): SettingsComponent.Factory {

    abstract val loggers: Set<LogcatLogger> // multi-bindings

    @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

    @Provides fun provideDebuggableApp(app: App): @Named("debuggableApp") Boolean = app.isDebuggable

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