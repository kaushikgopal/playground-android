package sh.kau.playground.usf.inspector

/**
 * Implementation of [UsfInspector] that chains multiple inspectors together in a composite pattern.
 *
 * Each method call is delegated to all contained inspectors in sequence, allowing for layered
 * inspection functionality. This follows the Composite design pattern, treating a group of
 * inspectors as a single inspector.
 *
 * This class is marked as internal and should not be instantiated directly. Instead, use:
 * - [UsfInspector.of] to create a composite inspector with appropriate implementation selection
 *
 * @param inspectors The list of inspectors to delegate to in order. If empty, behaves like
 *   [NoOpInspector].
 */
internal class CompositeInspector(private val inspectors: List<UsfInspector>) : UsfInspector {

  constructor(vararg inspectors: UsfInspector) : this(inspectors.toList())

  override fun onPipelineStarted() {
    inspectors.forEach { it.onPipelineStarted() }
  }

  override fun onPipelineStopped() {
    inspectors.forEach { it.onPipelineStopped() }
  }

  override fun onEvent(event: Any) {
    inspectors.forEach { it.onEvent(event) }
  }

  override fun onResult(result: Any) {
    inspectors.forEach { it.onResult(result) }
  }

  override fun onStateUpdated(state: Any) {
    inspectors.forEach { it.onStateUpdated(state) }
  }

  override fun onEffect(effect: Any) {
    inspectors.forEach { it.onEffect(effect) }
  }

  override fun error(error: Throwable, message: String) {
    inspectors.forEach { it.error(error, message) }
  }

  override fun debug(message: String) {
    inspectors.forEach { it.debug(message) }
  }
}
