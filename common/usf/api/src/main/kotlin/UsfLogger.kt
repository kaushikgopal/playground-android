package sh.kau.playground.usf

interface UsfLogger {
  fun verbose(message: String)

  fun debug(message: String)

  fun info(message: String)

  fun warning(message: String)

  fun error(error: Throwable?, message: String)

  // Consistently formatted logs for USF

  fun logEvent(event: Any) = verbose("[ev →   ] ${event.javaClass.simpleName}")

  fun logResult(result: Any) = verbose("[ev →  r] ${result.javaClass.simpleName}")

  fun logUiState(viewState: Any) = verbose("[vs →   ] ${viewState.javaClass.simpleName}")

  fun logEffect(effect: Any) = verbose("[ef →   ] ${effect.javaClass.simpleName}")

  fun logError(er: Throwable, msg: String) = error(er, "\uD83D\uDC80 error $msg")
}
