package sh.kau.playground.log

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import sh.kau.playground.shared.di.AppScope

/**
 * Only reason we have this vs using AndroidLogcatLogger directly is:
 * - control verbosity priority for Android logs
 * - control if logs sent for debuggable app
 * - demonstrate/show-off Metro multibinding + composite logging
 */
@Inject
@AppScope
@ContributesIntoSet(AppScope::class)
class AndroidLogger(
    @param:Named("debuggableApp") val debuggableApp: Boolean,
    androidLogger: AndroidLogcatLogger = AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger by androidLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && debuggableApp
}
