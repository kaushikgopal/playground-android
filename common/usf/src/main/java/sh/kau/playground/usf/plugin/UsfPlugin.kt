package sh.kau.playground.usf.plugin

import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter
import sh.kau.playground.usf.scope.ResultScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

/**
 * Implementation of the [UsfPluginInterface] interface with standard state and effect management.
 *
 * This abstract class provides the core infrastructure for plugin state management, event
 * processing, and effect emission. Concrete implementations need only define their initial state
 * and event processing logic.
 *
 * @param Event The plugin's event type
 * @param State The plugin's internal state type
 * @param Effect The plugin's effect type
 * @param initialScope Initial coroutine scope for the plugin (will be replaced when registered)
 * @param processingDispatcher Dispatcher used for event processing, defaults to IO
 * @param inspector Optional inspector for monitoring. If null, createInspector() will be called to
 *   create one.
 */
abstract class UsfPlugin<Event : Any, State : Any, Effect : Any>(
    initialScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    inspector: UsfInspector? = null,
) : UsfPluginInterface<Event, State, Effect>, UsfPluginRegistrar<Event, State, Effect> {

  private val _inspector: UsfInspector? by lazy { inspector ?: createInspector() }

  private val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
    _inspector?.error(e, "Uncaught exception in a child coroutine")
  }

  private var _coroutineScope: CoroutineScope = initialScope + handler

  /**
   * The current coroutine scope for this plugin.
   *
   * Use this scope for launching coroutines from within the plugin.
   */
  protected val coroutineScope: CoroutineScope
    get() = _coroutineScope

  private val _state by lazy { MutableStateFlow(initialState()) }
  final override val state: StateFlow<State> by lazy { _state.asStateFlow() }

  private val pluginRegistrar by lazy {
    UsfPluginRegistrarImpl<Event, State, Effect>(
        state = _state,
        coroutineScope = coroutineScope,
        inspector = _inspector,
    )
  }
  private val pluginEffects by lazy {
    pluginRegistrar.effects.onEach { effect -> inspectEffect(effect) }
  }

  private val _effects = MutableSharedFlow<Effect>()
  final override val effects: Flow<Effect> by lazy { merge(_effects, pluginEffects) }

  private val resultScope =
      object : ResultScope<State, Effect> {
        override fun updateState(update: (State) -> State) {
          val updatedState = _state.updateAndGet(update)
          coroutineScope.launch { _inspector?.onStateUpdated(updatedState) }
        }

        override fun emitEffect(effect: Effect) {
          emit(effect)
        }
      }

  final override fun input(event: Event) {
    // Process in all child plugins first
    pluginRegistrar.input(event)

    // Then process in parent plugin
    coroutineScope.launch(handler) {
      try {
        withContext(processingDispatcher) { resultScope.run { process(event) } }
      } catch (e: Exception) {
        if (e is CancellationException) throw e // propagate cancellation
        _inspector?.error(e, "[ev → s|ef]")
      }
      inspectEvent(event)
    }
  }

  /**
   * Creates an inspector for this plugin. This method is called only if no inspector was provided
   * in the constructor.
   *
   * Implementations should override this method to provide custom inspectors such as
   * AnalyticsInspector with access to implementation-specific state:
   * ```kotlin
   * override fun createInspector(): UsfInspector? {
   *   return AnalyticsInspector(
   *     stateProvider = { myImplementationSpecificState }
   *   )
   * }
   * ```
   *
   * @return The inspector to use, or null if no inspection is needed
   */
  protected open fun createInspector(): UsfInspector? = null

  /**
   * Provides the initial state for this plugin.
   *
   * Called once during initialization to establish the plugin's starting state. The returned state
   * becomes the first value emitted by the [state] flow.
   *
   * @return The initial state for this plugin
   */
  protected abstract fun initialState(): State

  /**
   * Processes an event to update state and emit effects.
   *
   * This method is called within the ResultScope context, providing direct access to updateState()
   * and emitEffect() methods. Override this method to handle events and produce state changes or
   * effects.
   *
   * @param event The event to process
   */
  protected open suspend fun ResultScope<State, Effect>.process(event: Event) {
    /* override to process events */
  }

  /**
   * Sets up the plugin with the provided isolated scope and calls the protected onRegistered method
   * with ResultScope access.
   *
   * @param coroutineScope The isolated scope provided by the parent component
   */
  final override fun onRegistered(coroutineScope: CoroutineScope) {
    _coroutineScope = coroutineScope + handler
    pluginRegistrar.register(_coroutineScope)
    try {
      with(resultScope) { onRegistered() }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      _inspector?.error(e, "[onRegistered]")
    }
  }

  /**
   * Called after plugin registration with access to the ResultScope.
   *
   * Override this method to perform initialization requiring state updates or effect emissions.
   */
  protected open fun ResultScope<State, Effect>.onRegistered() {
    // Default implementation does nothing
  }

  /** Performs standard cleanup by unregistering all child plugins. */
  final override fun onUnregistered() {
    pluginRegistrar.unregister()
  }

  private fun emit(effect: Effect) {
    coroutineScope.launch {
      _effects.emit(effect)
      inspectEffect(effect)
    }
  }

  /** Inspect an event using the inspector */
  private suspend fun inspectEvent(event: Event) {
    withContext(processingDispatcher) { _inspector?.onEvent(event) }
  }

  /** Inspect an effect using the inspector */
  private suspend fun inspectEffect(effect: Effect) {
    withContext(processingDispatcher) { _inspector?.onEffect(effect) }
  }

  /** Registers a child plugin with this plugin. */
  final override fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      adaptEvent: UsfEventAdapter<Event, PluginEvent>?,
      adaptState: UsfStateAdapter<PluginState, State>?,
      adaptEffect: UsfEffectAdapter<PluginEffect, Effect>?,
  ) {
    pluginRegistrar.register(plugin, adaptEvent, adaptState, adaptEffect)
  }

  /** Unregisters a previously registered child plugin. */
  final override fun <PluginEvent, PluginState, PluginEffect> unregister(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  ) {
    pluginRegistrar.unregister(plugin)
  }
}
