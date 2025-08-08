package sh.kau.playground.usf.plugin.adapter

import sh.kau.playground.usf.api.Usf
import sh.kau.playground.usf.plugin.UsfPluginInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Adapts any Usf implementation to the UsfPlugin interface.
 *
 * This adapter wraps a Usf component and exposes it as a plugin, handling the lifecycle methods and
 * delegating all operations to the underlying component.
 *
 * @param Event The event type of the component
 * @param State The state type of the component
 * @param Effect The effect type of the component
 * @param usf The Usf component to adapt
 */
class UsfPluginAdapter<Event : Any, State : Any, Effect : Any>(
    private val usf: Usf<Event, State, Effect>
) : UsfPluginInterface<Event, State, Effect> {

  private val _state = MutableStateFlow(usf.state.value)
  override val state: StateFlow<State> = _state.asStateFlow()

  // Buffer to prevent backpressure issues
  private val _effects = MutableSharedFlow<Effect>(replay = 0, extraBufferCapacity = 10)
  override val effects: Flow<Effect> = _effects

  /** Forwards events to the wrapped component. */
  override fun input(event: Event) {
    usf.input(event)
  }

  /**
   * Called when this plugin is registered with a parent component.
   *
   * This method starts collecting from the component's state and effects flows, ensuring that:
   * 1. The component is actively processing events
   * 2. State and effects are propagated to the parent
   * 3. Coroutine cancellation is properly handled
   *
   * @param coroutineScope The parent's coroutine scope to use for this plugin
   */
  override fun onRegistered(coroutineScope: CoroutineScope) {
    usf.state.onEach { state -> _state.value = state }.launchIn(coroutineScope)
    usf.effects.onEach { effect -> _effects.emit(effect) }.launchIn(coroutineScope)
  }

  /** Structured concurrency takes care of cleanup, no need to do anything here. */
  override fun onUnregistered() = Unit
}

/**
 * Extension function to convert any Usf implementation to a UsfPlugin.
 *
 * This makes it easy to use any Usf component as a plugin.
 *
 * @return A UsfPlugin adapter for this component
 */
fun <Event : Any, State : Any, Effect : Any> Usf<Event, State, Effect>.asPlugin():
    UsfPluginInterface<Event, State, Effect> {
  return UsfPluginAdapter(this)
}
