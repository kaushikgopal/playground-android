package sh.kau.playground.log

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import sh.kau.playground.shared.di.AppScope

/** Purely to demonstrate multi-binding + composite logging you shouldn't have this in a real app */
@Inject
@AppScope
@ContributesIntoSet(AppScope::class)
class AndroidLogger2(
    @param:Named("debuggableApp") val debuggableApp: Boolean,
    private val androidLogger: AndroidLogcatLogger =
        AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && debuggableApp

  override fun log(priority: LogPriority, tag: String, message: String) {
    androidLogger.log(priority, tag, "[logger 2] $message")
  }
}
