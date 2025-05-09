package sh.kau.playground.usf.api

interface UsfLogger {
  fun verbose(message: String)

  fun debug(message: String)

  fun info(message: String)

  fun warning(message: String)

  fun error(error: Throwable?, message: String)

  // Consistently formatted logs for USF

  fun logEvent(event: Any) = verbose("[ i →   ] ${event.javaClass.simpleName}")

  fun logResult(result: Any) = verbose("[ i →  o] ${result.javaClass.simpleName}")

  fun logUiState(viewState: Any) = verbose("[us →   ] ${viewState.javaClass.simpleName}")

  fun logEffect(effect: Any) = verbose("[ef →   ] ${effect.javaClass.simpleName}")

  fun logError(er: Throwable, msg: String) = error(er, "\uD83D\uDC80 error $msg")
}
