package sh.kau.playground.common.log

import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.domain.shared.di.AppScope
import sh.kau.playground.domain.shared.di.Named

/**
 * Only reason we have this vs using AndroidLogcatLogger directly is:
 * - control verbosity priority for Android logs
 * - control if logs sent for debuggable app
 * - demonstrate/show-off multibinding with kotlin-inject
 */
@AppScope
@Inject
// @SingleIn(AppScope::class)
// @ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger(
    @Named("debuggableApp") val debuggableApp: Boolean,
    androidLogger: AndroidLogcatLogger = AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger by androidLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && debuggableApp
}
