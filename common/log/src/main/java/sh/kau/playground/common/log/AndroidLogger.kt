package sh.kau.playground.common.log

import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.shared.App

/**
 * Only reason we have this vs using AndroidLogcatLogger directly is:
 * - control verbosity priority for Android logs
 * - control if logs sent for debuggable app
 * - demonstrate/show-off multibinding with kotlin-inject
 */
@Inject
// @SingleIn(AppScope::class)
// @ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger(
    private val app: App,
    androidLogger: AndroidLogcatLogger = AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger by androidLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && app.isDebuggable
}
