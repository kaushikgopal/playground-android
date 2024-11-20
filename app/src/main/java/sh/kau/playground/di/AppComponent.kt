package sh.kau.playground.di

import android.content.Context
import android.content.pm.ApplicationInfo
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.App
import sh.kau.playground.domain.shared.di.Named

@Component
abstract class ConfigComponent {

  @Provides fun provideAppName(): String = "My Playground!"

  @Provides
  fun provideDebugApp(app: App): @Named("debugApp") Boolean =
      (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Component
abstract class AppComponent(
    @get:Provides val app: App,
//    @Component val configComponent: ConfigComponent,
) {

//  abstract val loggers: Set<LogcatLogger>

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      return instance
          ?: AppComponent::class.create(
              context.applicationContext as App
          ).also { instance = it }
    }
  }
}
