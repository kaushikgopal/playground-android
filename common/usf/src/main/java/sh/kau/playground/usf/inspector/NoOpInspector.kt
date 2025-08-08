package sh.kau.playground.usf.inspector

/** A no-operation implementation of [UsfInspector] that performs no actions. */
object NoOpInspector : UsfInspector {

  override fun onPipelineStarted() = Unit

  override fun onPipelineStopped() = Unit

  override fun onEvent(event: Any) = Unit

  override fun onResult(result: Any) = Unit

  override fun onStateUpdated(state: Any) = Unit

  override fun onEffect(effect: Any) = Unit

  override fun error(error: Throwable, message: String) = Unit

  override fun debug(message: String) = Unit
}
