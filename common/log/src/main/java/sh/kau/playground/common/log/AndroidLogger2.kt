package sh.kau.playground.common.log

import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.App

/** Purely to demonstrate multi-binding + composite logging you shouldn't have this in a real app */
@Inject
// @SingleIn(AppScope::class)
// @ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger2(
    private val app: App,
    private val androidLogger: AndroidLogcatLogger =
        AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && app.isDebuggable

  override fun log(priority: LogPriority, tag: String, message: String) {
    androidLogger.log(priority, tag, "[logger 2] $message")
  }
}
