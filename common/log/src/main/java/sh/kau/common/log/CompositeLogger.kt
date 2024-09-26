package sh.kau.common.log

import android.app.Application
import android.content.pm.ApplicationInfo
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

class CompositeLogger(
    private val loggers: Set<LogcatLogger>, // [crashlyticsLogger, androidLogger]
) : LogcatLogger {

  override fun isLoggable(priority: LogPriority) = true

  override fun log(priority: LogPriority, tag: String, message: String) {
    loggers.forEach { logger ->
      if (logger.isLoggable(priority)) {
        logger.log(priority, tag, message)
      }
    }
  }

  companion object {
    fun install(
        app: Application,
        // loggers: Set<LogcatLogger>,
        minPriorityForDebuggableApp: LogPriority = LogPriority.DEBUG,
    ) {
      val loggers =
          setOf<LogcatLogger>(
              AndroidLogcatLogger(minPriorityForDebuggableApp),
          )

      if (!LogcatLogger.isInstalled && app.isDebuggableApp) {
        LogcatLogger.install(CompositeLogger(loggers))
      }
    }
  }
}

private val Application.isDebuggableApp: Boolean
  get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
