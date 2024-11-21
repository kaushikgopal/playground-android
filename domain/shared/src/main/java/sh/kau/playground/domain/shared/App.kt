package sh.kau.playground.domain.shared

/**
 * This is the main application interface.
 *
 * We use this interface in order to make the Application object across the DI graph without setting
 * up a reverse dependency back to the main :app module.
 */
interface App {
  val isDebuggable: Boolean
}
