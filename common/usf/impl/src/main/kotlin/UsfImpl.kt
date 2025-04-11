package sh.kau.playground.usf

import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
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
 * When building features, we create <Feature>ViewModelImpl.kt classes that extend this class. and
 * only implement the relevant parts of the feature. All boilerplate code is handled by
 * [sh.kau.playground.usf.UsfImpl] & the corresponding <Feature>UsfViewModel] class generated by the
 * annotation processor. This new version of the UsfViewModel also allows analytics event to be
 * automatically triggered for every Event and
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class UsfImpl<Event : Any, Result : Any, UiState : Any, Effect : Any, VMState : Any?>(
    initialUiState: UiState,
    private val coroutineScope: CoroutineScope,
    //    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val logger: UsfLogger = DefaultUsfLogger
) : Usf<Event, UiState, Effect> {

  private val concurrencyLimit = 100
  private var terminationJob: Job? = null
  private val timeoutValues = 5.seconds
  private val subscriberCount = AtomicInteger(0)
  internal val subscribers: Int
    get() = subscriberCount.get()

  val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
    logger.logError(e, "Uncaught exception in a child coroutine")
  }

  /**
   * @param event every input is processed into an [Event]
   * @return [Flow]<[Result]> a single [Event] can result in multiple [Result]s for e.g. emit a
   *   Result for loading and another for the actual result
   */
  protected abstract fun eventToResultFlow(event: Event): Flow<Result>

  /**
   * @param currentViewState the current [UiState] of the view (.copy it for the returned [UiState])
   * @return [UiState] Curiously, we don't return a [Flow]<[UiState]> here every [Result] will only
   *   ever be transformed into a single [UiState] if you want multiple [UiState]s emit multiple
   *   [Result]s transforming each [Result] to the respective [UiState]
   */
  protected abstract suspend fun resultToViewState(
      currentViewState: UiState,
      result: Result
  ): UiState

  /**
   * @param result a single [Result] can result in multiple [Effect]s for e.g. emit a VE for
   *   navigation and another for an analytics call hence a return type of [Flow]<[Effect]>
   * @return [Flow] of [Effect]s where null emissions will be ignored automatically
   */
  protected abstract fun resultToEffects(result: Result): Flow<Effect>

  /*
   * - Using a `Channel` allows us to buffer events emitted before the flow has started collecting.
   *
   *  This setup ensures that events are not lost if they are sent before the internal collector
   *   starts, and multiple events can be buffered until they are processed.
   */
  private val _events = Channel<Event>(10)

  /*
   * The `_viewState` holds the latest `ViewState` and replays it to new subscribers.
   */
  private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(initialUiState)
  override val uiState: StateFlow<UiState> =
      _uiState
          .manageSubscription(subscriberCount, ::startPipeline, ::schedulePipelineTermination)
          .onEach { logger.logUiState(it) }
          .stateIn(
              scope = coroutineScope,
              started = // strategy that controls when sharing is started and stopped
              SharingStarted.WhileSubscribed(
                      stopTimeoutMillis = 0, // on 0 subscribers, stop sharing immediately
                      replayExpirationMillis =
                          Long.MAX_VALUE, // on sharing stopped, never reset replay cache
                  ),
              initialValue = _uiState.value,
          )

  /*
   * - Using a `Channel` ensures once a value is exhausted, it won't be consumed again.
   */
  private val _effects = Channel<Effect>()
  override val effects: Flow<Effect> =
      _effects
          .receiveAsFlow()
          .manageSubscription(subscriberCount, ::startPipeline, ::schedulePipelineTermination)

  @VisibleForTesting internal var mainJob: Job? = null
  private val pipeline =
      _events
          .receiveAsFlow()
          .flatMapMerge(
              concurrencyLimit,
          ) { event ->
            try {
              eventToResultFlow(event).catch { logger.logError(it, "[ev →  r] flow") }
            } catch (e: Exception) {
              if (e is CancellationException) throw e // propagate cancellation
              logger.logError(e, "[ev → r]")
              emptyFlow()
            }
          }
          .onCompletion { logger.verbose("[ev →  r] ⏹") }
          //          .flowOn(processingDispatcher) // ↑ upstream will be executed in
          // processingDispatcher
          // ↓ downstream will be executed in current coroutineScope.plus(handler),
          // ↓ in app this will be the ViewModel scope [Dispatchers.Main.immediate]
          // ↓ in test this will be the scope passed in to the constructor
          .onEach { result ->
            coroutineScope.launch {
              try {
                _uiState.update { resultToViewState(it, result) }
                resultToEffects(result).collect {
                  _effects.send(it)
                  logger.logEffect(it)
                }
              } catch (e: Exception) {
                if (e is CancellationException) throw e // propagate cancellation
                logger.logError(e, "[r → ve]")
              }
            }
          }

  init {
    logger.debug("[-----VM] \uD83D\uDC76 on ${Thread.currentThread().name}")
  }

  open fun viewModelState(): VMState? = null

  override fun processInput(event: Event) {
    coroutineScope.launch(handler) {
      _events.send(event)
      withContext(Dispatchers.IO) { logger.logEvent(event) }
    }
  }

  private fun startPipeline() {
    if (mainJob == null || mainJob?.isActive != true) {
      logger.verbose("[-----VM] ▶")
      mainJob = pipeline.launchIn(coroutineScope.plus(handler))
    }
    terminationJob?.cancel()
    terminationJob = null
  }

  private fun schedulePipelineTermination() {
    terminationJob =
        coroutineScope.launch {
          delay(timeoutValues)
          if (subscriberCount.get() == 0) {
            mainJob?.cancel()
            mainJob = null
            logger.verbose("[-----VM] ⏹")
          }
        }
  }
}

/**
 * Extension function that manages subscription lifecycle for a Flow.
 *
 * @param subscriberCount Atomic counter tracking the number of active subscribers
 * @param startPipeline Function to call when the first subscriber starts collecting
 * @param schedulePipelineTermination Function to call when the last subscriber stops collecting
 * @return Flow with subscription management
 */
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
