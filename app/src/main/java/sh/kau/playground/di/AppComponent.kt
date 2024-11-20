package sh.kau.playground.di

import android.content.Context
import android.content.pm.ApplicationInfo
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import sh.kau.playground.App
import sh.kau.playground.domain.shared.di.Named

@Component
abstract class ConfigComponent(private val app: App) {

  @Provides fun provideAppName(): @Named("appName") String = "My Playground!"

  @Provides
  fun provideDebugApp(): @Named("debugApp") Boolean =
      (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Component
abstract class AppComponent(
    //    @get:Provides val app: App,
    @Component val configComponent: ConfigComponent, // component inheritance
) {

  //  abstract val loggers: Set<LogcatLogger>

  companion object {
    private var instance: AppComponent? = null

    fun from(context: Context): AppComponent {
      if (instance != null) return instance!!

      val app = context.applicationContext as App
      instance =
          AppComponent::class.create(
              // app,
              ConfigComponent::class.create(app),
          )

      return instance!!
    }
  }
}
