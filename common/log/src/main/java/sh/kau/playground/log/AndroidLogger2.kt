package sh.kau.playground.log

import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import me.tatarka.inject.annotations.Inject
import sh.kau.playground.shared.di.Named
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** Purely to demonstrate multi-binding + composite logging you shouldn't have this in a real app */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
class AndroidLogger2(
    @Named("debuggableApp") val debuggableApp: Boolean,
    private val androidLogger: AndroidLogcatLogger =
        AndroidLogcatLogger(LogPriority.VERBOSE), // usually DEBUG
) : LogcatLogger {

  override fun isLoggable(priority: LogPriority): Boolean =
      super.isLoggable(priority) && debuggableApp

  override fun log(priority: LogPriority, tag: String, message: String) {
    androidLogger.log(priority, tag, "[logger 2] $message")
  }
}