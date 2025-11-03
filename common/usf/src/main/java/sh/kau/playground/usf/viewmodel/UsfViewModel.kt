package sh.kau.playground.usf.viewmodel

import sh.kau.playground.usf.api.Usf
import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.plugin.UsfPluginInterface
import sh.kau.playground.usf.plugin.UsfPluginRegistrar
import sh.kau.playground.usf.plugin.UsfPluginRegistrarImpl
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter
import sh.kau.playground.usf.scope.ResultScope
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

/**
 * Base implementation of UsfViewModel that adopts the ResultScope pattern with pipeline-based
 * lifecycle management.
 *
 * Key features:
 * 1. Direct event processing without intermediate Results
 * 2. ResultScope for state updates and effect emissions
 * 3. Support for plugin composition
 * 4. Automatic pipeline management based on subscriber count
 * 5. Timeout-based cleanup when no subscribers remain
 *
 * The pipeline architecture ensures resources are only active when needed - when the first
 * subscriber starts collecting, the pipeline activates and plugins are registered. When the last
 * subscriber stops collecting, a timeout begins. If no new subscribers appear within the timeout,
 * all plugins are unregistered and the pipeline shuts down.
 *
 * @param Event The input event type
 * @param UiState The UI state type
 * @param Effect The effect type for one-time actions
 * @param coroutineScope The scope to run coroutines in
 * @param processingDispatcher The dispatcher to use for event processing, defaults to IO
 * @param inspector Optional inspector for monitoring. If null, createInspector() will be called to
 *   create one.
 */
abstract class UsfViewModel<Event : Any, UiState : Any, Effect : Any>(
    coroutineScope: CoroutineScope,
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    inspector: UsfInspector? = null,
) : Usf<Event, UiState, Effect>, UsfPluginRegistrar<Event, UiState, Effect> {

  private val viewModelMaxTimeoutSeconds = 5.seconds
  internal val subscriberCount = AtomicInteger(0)

  private var _viewModelScope: CoroutineScope = coroutineScope
  private var _pipelineScope: CoroutineScope? = null

  /**
   * The current coroutine scope for this view model.
   *
   * When the pipeline is active, this returns the pipeline scope where jobs are automatically
   * canceled when the pipeline terminates. When the pipeline is inactive, this returns the view
   * model scope where jobs live for the view model's entire lifetime.
   */
  protected val coroutineScope: CoroutineScope
    get() = _pipelineScope ?: _viewModelScope

  private var terminationJob: Job? = null

  private val _inspector: UsfInspector? by lazy { inspector ?: createInspector() }

  private val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
    _inspector?.error(e, "Uncaught exception in a child coroutine")
  }

  /*
   * - Using a `Channel` ensures once a value is exhausted, it won't be consumed again.
   */
  private val eventsChannel = Channel<Event>(10)

  private val _state: MutableStateFlow<UiState> by lazy { MutableStateFlow(initialState()) }
  final override val state: StateFlow<UiState> by lazy {
    _state
        .manageSubscription(subscriberCount, ::startPipeline, ::schedulePipelineTermination)
        .onEach { _inspector?.onStateUpdated(it) }
        .stateIn(
            scope = _viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 0,
                    replayExpirationMillis = Long.MAX_VALUE,
                ),
            initialValue = initialState(),
        )
  }

  private val pluginRegistrar by lazy {
    UsfPluginRegistrarImpl<Event, UiState, Effect>(
        state = _state,
        coroutineScope = _viewModelScope,
        inspector = _inspector,
        parentInput = { event -> input(event) },
    )
  }

  private val pluginEffects by lazy {
    pluginRegistrar.effects.onEach { effect ->
      _viewModelScope.launch(handler + processingDispatcher) { _inspector?.onEffect(effect) }
    }
  }

  private val _effects = Channel<Effect>()
  final override val effects: Flow<Effect> by lazy {
    merge(_effects.receiveAsFlow(), pluginEffects)
        .manageSubscription(subscriberCount, ::startPipeline, ::schedulePipelineTermination)
  }

  // ResultScope implementation with access to current state
  private val resultScope =
      object : ResultScope<UiState, Effect> {
        override fun updateState(update: (UiState) -> UiState) {
          val scope = _pipelineScope ?: _viewModelScope
          if (!Dispatchers.Main.immediate.isDispatchNeeded(scope.coroutineContext)) {
            _state.update(update)
          } else {
            scope.launch(handler + Dispatchers.Main.immediate) { _state.update(update) }
          }
        }

        override fun emitEffect(effect: Effect) {
          emit(effect)
        }

        override suspend fun <T> offload(block: suspend () -> T): T {
          return withContext(processingDispatcher) { block() }
        }
      }

  @VisibleForTesting internal var mainJob: Job? = null
  private val pipeline =
      eventsChannel
          .receiveAsFlow()
          .onStart { _inspector?.onPipelineStarted() }
          .onCompletion { _inspector?.onPipelineStopped() }
          .onEach { event ->
            _pipelineScope?.launch(handler + Dispatchers.Main.immediate) {
              launch(processingDispatcher) { _inspector?.onEvent(event) }
              try {
                resultScope.run { process(event) }
              } catch (e: Exception) {
                if (e is CancellationException) throw e
                _inspector?.error(e, "[ev → s|e]")
              }
            }
          }

  /** Process an input event by sending it to the events channel. */
  override fun input(event: Event) {
    _viewModelScope.launch(handler) {
      // Process in all plugins
      pluginRegistrar.input(event)

      // Also process in the view model
      eventsChannel.send(event)
    }
  }

  /**
   * Creates an inspector for this view model. This method is called only if no inspector was
   * provided in the constructor.
   *
   * Implementations should override this method to provide custom inspectors such as
   * AnalyticsInspector with access to implementation-specific state:
   * ```kotlin
   * override fun createInspector(): UsfInspector? {
   *   return AnalyticsInspector(
   *     analyticsApi = analyticsApi,
   *     stateProvider = { myImplementationSpecificState }
   *   )
   * }
   * ```
   *
   * @return The inspector to use, or null if no inspection is needed
   */
  protected open fun createInspector(): UsfInspector? = null

  /**
   * Provides the initial state for this view model.
   *
   * This method is called exactly once during initialization to establish the view model's starting
   * state. The state returned by this method becomes the first value emitted by the [state]
   * StateFlow and serves as the foundation for all subsequent state updates.
   *
   * When implementing this method:
   * 1. Return an immutable state object that represents the initial UI state
   * 2. Include sensible default values for all required properties
   * 3. Consider loading states (e.g., isLoading=true) if appropriate
   * 4. Ensure the state is complete and valid, as it will be the first state observed by the UI
   *
   * This method is intentionally abstract to force implementations to explicitly define their
   * initial state, rather than relying on default values that might be overlooked.
   *
   * @return The initial state for this view model
   */
  protected abstract fun initialState(): UiState

  /**
   * Processes an event within the ResultScope context to update state and emit effects.
   *
   * This is the main event handling method that subclasses must implement. When an event is
   * received, this method is called with ResultScope as the receiver, providing direct access to
   * state updates and effect emissions through the updateState() and emitEffect() methods.
   *
   * The implementation should:
   * 1. Determine if the event can be handled by this view model
   * 2. Update state using updateState() as needed
   * 3. Emit effects using emitEffect() as needed
   *
   * @param event The event to process
   */
  protected abstract suspend fun ResultScope<UiState, Effect>.process(event: Event)

  /**
   * Called when the view model pipeline starts up with access to the ResultScope.
   *
   * Override this method to perform initialization requiring state updates or effect emissions when
   * the first subscriber starts collecting from the view model's state or effects flows. This is
   * called each time the pipeline starts, which happens when transitioning from zero to one
   * subscriber.
   *
   * Jobs launched on `coroutineScope` within this method will be automatically tied to the pipeline
   * lifecycle and canceled when the pipeline terminates. To launch jobs that survive pipeline
   * termination, you would need to explicitly use a different scope.
   */
  protected open fun ResultScope<UiState, Effect>.onSubscribed() {
    // Default implementation does nothing
  }

  /** Starts the event processing pipeline and connects all registered plugins. */
  private fun startPipeline() {
    if (mainJob == null || mainJob?.isActive != true) {
      // Create a pipeline-scoped coroutine scope that's a child of _viewModelScope
      // This ensures _pipelineScope gets canceled when _viewModelScope is canceled
      _pipelineScope =
          CoroutineScope(
              _viewModelScope.coroutineContext +
                  SupervisorJob(_viewModelScope.coroutineContext[Job]))

      mainJob = pipeline.launchIn(_viewModelScope.plus(handler))
      pluginRegistrar.register(_pipelineScope!!)

      try {
        with(resultScope) { onSubscribed() }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _inspector?.error(e, "[onSubscribed]")
      }
    }
    terminationJob?.cancel()
    terminationJob = null
  }

  /**
   * Schedules pipeline termination after the timeout period if no subscribers remain.
   *
   * This method is automatically called when the last subscriber stops collecting. If no new
   * subscribers appear within the timeout period, all plugins are notified via onUnregistered() and
   * the pipeline is shut down.
   */
  private fun schedulePipelineTermination() {
    terminationJob =
        _viewModelScope.launch(handler) {
          delay(viewModelMaxTimeoutSeconds)
          if (subscriberCount.get() == 0) {
            pluginRegistrar.unregister()

            mainJob?.cancel()
            mainJob = null

            // Cancel the pipeline scope and all its child jobs
            _pipelineScope?.cancel()
            _pipelineScope = null
          }
        }
  }

  /** Extension function that manages subscription lifecycle for a Flow. */
  private fun <T> Flow<T>.manageSubscription(
      subscriberCount: AtomicInteger,
      startPipeline: () -> Unit,
      schedulePipelineTermination: () -> Unit
  ): Flow<T> {
    return this.onStart {
          if (subscriberCount.incrementAndGet() == 1) {
            startPipeline()
          }
        }
        .onCompletion {
          if (subscriberCount.decrementAndGet() == 0) {
            schedulePipelineTermination()
          }
        }
  }

  private fun emit(effect: Effect) {
    _viewModelScope.launch(handler + Dispatchers.Main.immediate) {
      _effects.send(effect)
      launch(processingDispatcher) { _inspector?.onEffect(effect) }
    }
  }

  /** Registers a child plugin with this view model. */
  final override fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      mapEvent: UsfEventAdapter<Event, PluginEvent>?,
      applyState: UsfStateAdapter<PluginState, UiState>?,
      mapEffect: UsfEffectAdapter<PluginEffect, Effect>?,
      transformEffect: UsfEffectToEventAdapter<PluginEffect, Event>?
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
