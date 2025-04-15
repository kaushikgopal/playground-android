package sh.kau.playground.log.usf

import logcat.LogPriority.ERROR
import logcat.LogPriority.INFO
import logcat.LogPriority.VERBOSE
import logcat.LogPriority.WARN
import logcat.asLog
import logcat.logcat
import sh.kau.playground.usf.api.UsfLogger

object DefaultUsfLogger : UsfLogger {

  override fun verbose(message: String) = logcat(VERBOSE) { message }

  override fun debug(message: String) = logcat { message }

  override fun info(message: String) = logcat(INFO) { message }

  override fun warning(message: String) = logcat(WARN) { message }

  override fun error(error: Throwable?, message: String) =
      logcat(ERROR) { (error ?: RuntimeException("Error in USF land")).asLog() }
}
