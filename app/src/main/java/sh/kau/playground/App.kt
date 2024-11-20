package sh.kau.playground

import android.app.Application
import logcat.LogPriority
import logcat.logcat
import sh.kau.playground.common.log.CompositeLogger
import sh.kau.playground.di.AppComponent

class App : Application() {

  private val appComponent by lazy(LazyThreadSafetyMode.NONE) { AppComponent.from(this) }

  override fun onCreate() {
    super.onCreate()

    // Log all priorities in debug builds, no-op in release builds.
    // AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    CompositeLogger.install(appComponent.loggers)

    // calling the field directly on appComponent
    // is made possible because AppComponent inherits KotlinInjectAppComponentMerged directly
    // you could alternatively provide another injection intermediate object
    logcat(LogPriority.INFO) { "xxx Welcome to ${appComponent.configComponent.provideAppName()}" }

    logcat { "number of loggers: ${appComponent.loggers.size}" }
  }
}
