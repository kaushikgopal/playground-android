package sh.kau.playground.usf

import sh.kau.playground.usf.api.UsfLogger

object TestUsfLogger : UsfLogger {
  override fun verbose(message: String) = println(message)

  override fun debug(message: String) = println(message)

  override fun info(message: String) = println(message)

  override fun warning(message: String) = println(message)

  override fun error(error: Throwable?, message: String) = println(message)
}
