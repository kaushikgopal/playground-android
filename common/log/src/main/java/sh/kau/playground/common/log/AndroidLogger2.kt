package sh.kau.playground.common.log

import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.shared.di.Named

/** Purely to demonstrate multi-binding + composite logging you shouldn't have this in a real app */
@Inject
// @SingleIn(AppScope::class)
// @ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger2(
    @Named("debugApp") private val isDebuggableApp: Boolean,
    private val androidLogger: AndroidLogcatLogger =
        AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && isDebuggableApp

  override fun log(priority: LogPriority, tag: String, message: String) {
    androidLogger.log(priority, tag, "[logger 2] $message")
  }
}
