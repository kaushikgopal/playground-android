package sh.kau.playground.usf.plugin

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter
import sh.kau.playground.usf.scope.ResultScope

/**
 * Implementation of the [UsfPluginInterface] interface with standard state and effect management.
 *
 * This abstract class provides the core infrastructure for plugin state management, event
 * processing, and effect emission. Concrete implementations need only define their initial state
 * and event processing logic.
 *
 * Key characteristics:
 * - Events are processed on the main thread (Dispatchers.Main.immediate)
 * - Heavy work should be offloaded using ResultScope.offload()
 * - Events can be sent before start() is called (they are buffered)
 * - Effects use Channel for one-time delivery semantics
 *
 * @param Event The plugin's event type
 * @param State The plugin's internal state type
 * @param Effect The plugin's effect type
 * @param processingDispatcher Dispatcher used for offloading heavy work, defaults to IO
 * @param inspector Optional inspector for monitoring. If null, createInspector() will be called to
 *   create one.
 */
abstract class UsfPlugin<Event : Any, State : Any, Effect : Any>(
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    inspector: UsfInspector? = null,
) : UsfPluginInterface<Event, State, Effect>, UsfPluginRegistrar<Event, State, Effect> {

  private val _inspector: UsfInspector? by lazy { inspector ?: createInspector() }

  private val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
    _inspector?.error(e, "Uncaught exception in a child coroutine")
  }

  /*
   * Using a Channel ensures events sent before start() are buffered.
   * This matches the main ViewModel's behavior and allows events to be sent anytime.
   */
  private var eventsChannel = Channel<Event>(10)

  /*
   * A plugin-owned scope used for event buffering via input().
   * This scope is created lazily on first input and recreated on re-start if cancelled.
   * It is cancelled on stop to prevent resource leaks.
   */
  private var eventBufferScope: CoroutineScope? = null

  /*
   * The job responsible for processing events from the eventsChannel.
   * This job is started when the plugin is started and cancelled when stopped.
   * Each event is processed in its own coroutine for isolated error handling and concurrent
   * processing. Null when the plugin is not started.
   */
  private var eventProcessingJob: Job? = null

  private var _coroutineScope: CoroutineScope? = null

  /**
   * The current coroutine scope for this plugin.
   *
   * Use this scope for launching coroutines from within the plugin. Will throw an error if accessed
   * before the plugin is started.
   */
  protected val coroutineScope: CoroutineScope
    get() = _coroutineScope ?: error("Plugin must be started before accessing coroutineScope")

  private val _state by lazy { MutableStateFlow(initialState()) }
  final override val state: StateFlow<State> by lazy { _state.asStateFlow() }

  private val pluginRegistrar by lazy {
    UsfPluginRegistrarImpl<Event, State, Effect>(
        state = _state,
        coroutineScope = _coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default),
        inspector = _inspector,
        parentInput = { event -> input(event) },
    )
  }

  /*
   * Using a Channel ensures once a value is consumed, it won't be replayed to new collectors.
   * This is important for effects which are typically one-time events (like navigation).
   * The Channel will buffer effects emitted during onStarted() before collectors start.
   */
  private val _effects = Channel<Effect>(10)
  final override val effects: Flow<Effect> by lazy { _effects.receiveAsFlow() }

  private val resultScope =
      object : ResultScope<State, Effect> {
        override fun updateState(update: (State) -> State) {
          val scope = _coroutineScope ?: return
          if (!Dispatchers.Main.immediate.isDispatchNeeded(scope.coroutineContext)) {
            val updatedState = _state.updateAndGet(update)
            scope.launch(handler + processingDispatcher) {
              _inspector?.onStateUpdated(updatedState)
            }
          } else {
            scope.launch(handler + Dispatchers.Main.immediate) {
              val updatedState = _state.updateAndGet(update)
              launch(processingDispatcher) { _inspector?.onStateUpdated(updatedState) }
            }
          }
        }

        override fun emitEffect(effect: Effect) {
          val scope = _coroutineScope ?: return
          scope.launch(handler + Dispatchers.Main.immediate) {
            _effects.send(effect)
            launch(processingDispatcher) { _inspector?.onEffect(effect) }
          }
        }

        override suspend fun <T> offload(block: suspend () -> T): T {
          return withContext(processingDispatcher) { block() }
        }
      }

  /**
   * Ensures the event buffer scope exists and is active. Creates a new scope if it doesn't exist or
   * was cancelled.
   *
   * If the plugin has been started (i.e., _coroutineScope is set), the buffer scope inherits the
   * dispatcher from the parent scope. This ensures proper behavior with test dispatchers. Before
   * start, uses Dispatchers.Default for buffering.
   */
  private fun ensureEventBufferScope(): CoroutineScope {
    val currentScope = eventBufferScope
    return if (currentScope != null && currentScope.isActive) {
      currentScope
    } else {
      // Inherit dispatcher from parent scope if available (important for tests)
      val dispatcher =
          _coroutineScope?.coroutineContext?.get(CoroutineDispatcher) ?: Dispatchers.Default
      CoroutineScope(SupervisorJob() + dispatcher + handler).also { eventBufferScope = it }
    }
  }

  /**
   * Submits an event for processing by this plugin.
   *
   * This is the primary entry point for sending events to the plugin. The event is processed
   * asynchronously, first through all registered child plugins, then through this plugin's
   * process() method.
   *
   * Key characteristics:
   * - Thread-safe: can be called from any thread
   * - Non-blocking: returns immediately, event is buffered/processed asynchronously
   * - Isolated error handling: exceptions don't propagate to the caller
   * - Can be called before start(): events are buffered until the plugin starts
   *
   * @param event The event to process
   */
  final override fun input(event: Event) {
    ensureEventBufferScope().launch(handler) { eventsChannel.send(event) }
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
   * This method runs on Dispatchers.Main.immediate. Use offload {} for heavy work.
   *
   * @param event The event to process
   */
  protected open suspend fun ResultScope<State, Effect>.process(event: Event) {
    /* override to process events */
  }

  /**
   * Starts the plugin with the provided isolated scope and calls the protected onStarted method
   * with ResultScope access.
   *
   * @param coroutineScope The isolated scope provided by the parent component
   */
  final override fun start(coroutineScope: CoroutineScope) {
    _coroutineScope = coroutineScope

    // Cancel existing buffer scope to recreate with correct dispatcher (important for tests)
    eventBufferScope?.cancel()
    eventBufferScope = null

    // Create event buffer scope with correct dispatcher from parent scope
    ensureEventBufferScope()

    // Cancel any existing event processing (in case of re-start)
    eventProcessingJob?.cancel()

    // Start processing events from the buffer
    eventProcessingJob = startEventProcessing(coroutineScope)

    // Start forwarding plugin effects to _effects Channel BEFORE starting child plugins
    // so any effects emitted during plugin start (e.g., in onStarted()) are captured.
    coroutineScope.launch(handler) {
      pluginRegistrar.effects.collect {
        _effects.send(it)
        _inspector?.onEffect(it)
      }
    }

    pluginRegistrar.start(coroutineScope)

    try {
      with(resultScope) { onStarted() }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      _inspector?.error(e, "[onStarted]")
    }
  }

  /**
   * Starts processing events from the event channel.
   *
   * Events are collected from the channel and processed on the main thread, with each event
   * launched in its own coroutine for isolated error handling and independent cancellation.
   *
   * @param scope The coroutine scope to use for event processing
   * @return A Job representing the event processing coroutine
   */
  private fun startEventProcessing(scope: CoroutineScope): Job {
    return scope.launch(handler) {
      eventsChannel.receiveAsFlow().collect { event ->
        // Launch a coroutine per event for:
        // 1. Concurrent processing - events can be processed in parallel
        // 2. Isolated error handling - exceptions in one event don't kill the pipeline
        // 3. Independent cancellation - each event can be cancelled separately
        //
        // IMPORTANT: Process on Main.immediate to keep main-thread-first model
        scope.launch(handler + Dispatchers.Main.immediate) {
          _inspector?.onEvent(event)

          // Process in all child plugins first
          pluginRegistrar.input(event)

          // Then process in parent plugin
          try {
            _inspector?.beforeProcessEvent(event)
            resultScope.run { process(event) }
            _inspector?.afterProcessEvent(event)
          } catch (e: Exception) {
            if (e is CancellationException) throw e // propagate cancellation
            _inspector?.error(e, "[ev → s|ef]")
          }
        }
      }
    }
  }

  /**
   * Called after plugin start with access to the ResultScope.
   *
   * Override this method to perform initialization requiring state updates or effect emissions.
   *
   * Any coroutines launched from within this method will run on Main.immediate by default.
   */
  protected open fun ResultScope<State, Effect>.onStarted() {
    // Default implementation does nothing
  }

  /**
   * Called after the plugin stops.
   *
   * Override this method to perform cleanup operations after the plugin is stopped.
   */
  protected open fun onStopped() {
    // Default implementation does nothing
  }

  /**
   * Stops the plugin, performing standard cleanup by stopping all child plugins and nulling out the
   * scope.
   *
   * After stopping, the plugin stops processing events and clears any buffered events. The event
   * buffer scope is cancelled to prevent resource leaks. Events sent via [input] after stopping
   * will be dropped until the plugin is re-started.
   */
  final override fun stop() {
    eventProcessingJob?.cancel()
    eventProcessingJob = null

    // Cancel and clear the event buffer scope to prevent resource leaks
    eventBufferScope?.cancel()
    eventBufferScope = null

    // Clear any buffered events by recreating the channel
    eventsChannel.cancel()
    eventsChannel = Channel(10)

    pluginRegistrar.stop()
    _coroutineScope = null

    onStopped()
  }

  /** Registers a child plugin with this plugin. */
  final override fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      mapEvent: UsfEventAdapter<Event, PluginEvent>?,
      applyState: UsfStateAdapter<PluginState, State>?,
      mapEffect: UsfEffectAdapter<PluginEffect, Effect>?,
      transformEffect: UsfEffectToEventAdapter<PluginEffect, Event>?,
  ) {
    pluginRegistrar.register(plugin, mapEvent, applyState, mapEffect, transformEffect)
  }

  /** Unregisters a previously registered child plugin. */
  final override fun <PluginEvent, PluginState, PluginEffect> unregister(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  ) {
    pluginRegistrar.unregister(plugin)
  }
}
