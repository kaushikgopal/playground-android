package sh.kau.playground.usf.log

import logcat.LogPriority
import logcat.LogPriority.ERROR
import logcat.LogPriority.INFO
import logcat.LogPriority.VERBOSE
import logcat.LogPriority.WARN
import logcat.asLog
import logcat.logcat
import sh.kau.playground.usf.inspector.UsfInspector

/**
 * Android-specific implementation of UsfInspector that logs USF lifecycle events to Logcat.
 *
 * Intent: Provide high-signal, low-noise visibility into the USF pipeline without coupling
 * non-Android code to Android logging. This class lives in an Android module so core USF remains
 * UI- and platform-agnostic. The app is expected to install a LogcatLogger (potentially a
 * CompositeLogger) during Application startup to route logs according to build type and policy.
 *
 * Assumptions:
 * - A LogcatLogger has been installed by the app at startup; otherwise, logcat() will install a
 *   default logger. In either case, logging here must never crash or block.
 * - Logging happens on whatever thread the USF pipeline calls into; messages are small and
 *   formatting is constant-time.
 * - The string markers used here are intentionally consistent across the app to aid grepping.
 */
open class UsfLoggingInspector(val prefix: String = "[USF]") : UsfInspector {

  override fun debug(message: String) = logcat { "$prefix$message" }

  override fun error(error: Throwable, message: String) =
      logcat(LogPriority.ERROR) { "$prefix \uD83D\uDC80 | $message | ${error.asLog()} " }

  private fun verbose(message: String) = logcat(VERBOSE) { "$prefix$message" }

  private fun info(message: String) = logcat(INFO) { "$prefix$message" }

  private fun warning(message: String) = logcat(WARN) { "$prefix$message" }

  private fun logEvent(event: Any) = verbose("[  i →    ] ${event.javaClass.simpleName}")

  private fun logResult(result: Any) = verbose("[  i → o  ] ${result.javaClass.simpleName}")

  private fun logUiState(viewState: Any) = verbose("[ us →    ] ${viewState.javaClass.simpleName}")

  private fun logEffect(effect: Any) = verbose("[ ef →    ] ${effect.javaClass.simpleName}")

  override fun onPipelineStarted() {
    // Marks activation of the pipeline (first subscriber present).
    verbose("[         ] ▶")
  }

  override fun onPipelineStopped() {
    // Marks termination of the pipeline (no more subscribers after timeout).
    verbose("[ev → s|ef] ⏹")
  }

  override fun onEvent(event: Any) {
    // Events enter the pipeline and are handled synchronously in order.
    logEvent(event)
  }

  override fun onResult(result: Any) = logResult(result)

  override fun onStateUpdated(state: Any) = logUiState(state)

  override fun onEffect(effect: Any) = logEffect(effect)
}
