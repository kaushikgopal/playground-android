package sh.kau.playground

import android.app.Application
import logcat.LogPriority
import logcat.logcat
import sh.kau.common.log.CompositeLogger
import sh.kau.playground.di.AppComponent

class App : Application() {

  private val appComponent by lazy(LazyThreadSafetyMode.NONE) { AppComponent.from(this) }

  override fun onCreate() {
    super.onCreate()

    // Log all priorities in debug builds, no-op in release builds.
    // AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    CompositeLogger.install(this)

    logcat(LogPriority.INFO) { "xxx Welcome to ${appComponent.provideAppName()}" }
  }
}
