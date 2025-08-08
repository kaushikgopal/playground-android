package sh.kau.playground.usf.inspector

/**
 * Interface for inspecting USF (Unidirectional State Flow) components (events, results, state,
 * effects).
 *
 * Provides a unified approach to handling logging, debugging, and analytics tracking across USF
 * components. This replaces direct use of logger and analytics API in USF components.
 *
 * The inspector follows the lifecycle of a USF component and provides hooks at each stage of the
 * processing pipeline:
 * 1. Pipeline lifecycle (start/stop)
 * 2. Event receipt
 * 3. Result production
 * 4. State updates
 * 5. Effect emissions
 * 6. Error handling
 */
interface UsfInspector {

  /** Called when the USF pipeline starts. */
  fun onPipelineStarted()

  /** Called when the USF pipeline stops. */
  fun onPipelineStopped()

  /**
   * Inspects an event that was received by a USF component.
   *
   * @param event The event that was received
   */
  fun onEvent(event: Any)

  /**
   * Inspects a result that was produced during event processing. For backward compatibility with
   * older USF implementations.
   *
   * @param result The result that was produced
   */
  fun onResult(result: Any)

  /**
   * Inspects a state update in a USF component.
   *
   * @param state The new state after the update
   */
  fun onStateUpdated(state: Any)

  /**
   * Inspects an effect that was emitted by a USF component.
   *
   * @param effect The effect that was emitted
   */
  fun onEffect(effect: Any)

  /**
   * Logs an error that occurred during event processing.
   *
   * @param error The error that occurred
   * @param message Optional message providing context about the error
   */
  fun error(error: Throwable, message: String)

  /**
   * Logs a debug message during the USF pipeline execution.
   *
   * Useful for providing additional context or information during development or troubleshooting
   * without affecting the normal flow of operations.
   *
   * @param message The debug message to log
   */
  fun debug(message: String)

  companion object {
    /**
     * Creates a UsfInspector from the provided inspectors.
     *
     * This factory method automatically chooses the most appropriate implementation:
     * - If no inspectors are provided, returns NoOpInspector
     * - If one inspector is provided, returns that inspector
     * - If multiple inspectors are provided, returns a CompositeInspector
     *
     * @param inspectors The inspectors to combine into a single inspector
     * @return An appropriate UsfInspector implementation
     */
    fun of(vararg inspectors: UsfInspector): UsfInspector {
      return when {
        inspectors.isEmpty() -> NoOpInspector
        inspectors.size == 1 -> inspectors.first()
        else -> CompositeInspector(*inspectors)
      }
    }
  }
}
