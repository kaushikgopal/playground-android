package sh.kau.playground.app

import android.app.Application
import android.content.pm.ApplicationInfo
import logcat.LogPriority
import logcat.logcat
import sh.kau.playground.app.di.createAppGraph
import sh.kau.playground.log.CompositeLogger
import sh.kau.playground.shared.App

class AppImpl : App, Application() {

  private val appGraph by lazy(LazyThreadSafetyMode.NONE) { createAppGraph(this) }

  override val isDebuggable: Boolean
    get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

  override fun onCreate() {
    super.onCreate()

    if (isDebuggable) {
      try {
        StrictModeInitializer.enableStrictMode()
      } catch (_: Throwable) {
        // StrictMode initializer is debug-only; never crash startup if it fails.
      }
    }

    // Log all priorities in debug builds, no-op in release builds.
    // AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    CompositeLogger.install(appGraph.loggers.value)

    logcat(LogPriority.INFO) { "xxx Welcome to ${appGraph.appName}" }

    logcat { "number of loggers: ${appGraph.loggers.value.size}" }
  }
}
