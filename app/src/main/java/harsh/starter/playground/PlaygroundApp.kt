package harsh.starter.playground

import android.app.Application
import android.content.pm.ApplicationInfo
import logcat.LogPriority
import logcat.logcat
import harsh.starter.playground.common.log.CompositeLogger
import harsh.starter.playground.di.AppComponent
import harsh.starter.playground.domain.shared.App

class PlaygroundApp : App, Application() {

  private val appComponent by lazy(LazyThreadSafetyMode.NONE) { AppComponent.from(this) }

  override val isDebuggable: Boolean
    get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

  override fun onCreate() {
    super.onCreate()

    // Log all priorities in debug builds, no-op in release builds.
    // AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    CompositeLogger.install(appComponent.loggers)

    // calling the field directly on appComponent
    // is made possible because AppComponent inherits KotlinInjectAppComponentMerged directly
    // you could alternatively provide another injection intermediate object
    logcat(LogPriority.INFO) { "xxx Welcome to ${appComponent.provideAppName()}" }

    logcat { "number of loggers: ${appComponent.loggers.size}" }
  }
}
