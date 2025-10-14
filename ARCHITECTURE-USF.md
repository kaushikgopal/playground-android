# USF (Unidirectional State Flow) Architecture

The USF pattern is perfect for:
- Screens with user interactions
- Complex state management
- Async operations with loading states
- Features needing predictable testing

## Quick Overview

USF enforces a simple, predictable pattern: **Events → Process → State/Effects**

Think MVI with built-in lifecycle management and less boilerplate.

**Traditional ViewModel:**
```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun onButtonClick() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            // Complex logic mixed with state updates
        }
    }
}
```

**USF ViewModel:**
```kotlin
class MyViewModel(
    coroutineScope: CoroutineScope
) : UsfViewModel<Event, UiState, Effect>(coroutineScope) {
    override fun initialState() = UiState()

    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        when (event) {
            is Event.ButtonClick -> {
                updateState { it.copy(loading = true) }
                // Clear separation of concerns
            }
        }
    }
}
```

## Core Components

### The Three Types

```kotlin
// 1. Events - User actions
sealed interface HomeEvent {
    data object RefreshClicked : HomeEvent
    data class WelcomeMessageClicked(val message: String) : HomeEvent
}

// 2. State - UI display (with callbacks)
data class HomeUiState(
    val tabTitle: String = "Home",
    val lastRefreshTime: String? = null,
    // Callbacks as part of state
    val onRefreshClicked: () -> Unit = {},
    val onWelcomeMessageClicked: (String) -> Unit = {},
)

// 3. Effects - One-time actions
sealed interface HomeEffect {
    data object NavigateToSettings : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}
```

### Naming Convention

For screens with longer names, use abbreviated prefixes for Event/State/Effect types:

```kotlin
// For SettingsAScreen
sealed interface SAEvent { ... }
data class SAUiState(...)
sealed interface SAEffect { ... }

// For SettingsBScreen
sealed interface SBEvent { ... }
data class SBUiState(...)
sealed interface SBEffect { ... }
```

## File Structure

Each USF feature follows a consistent module organization:

```
features/myfeature/
├── src/main/java/app/pudi/android/myfeature/
│   ├── ui/
│   │   ├── MyScreen.kt                 // Composable screen
│   │   ├── MyViewModel.kt              // Interface
│   │   └── MyViewModelImpl.kt          // USF implementation
│   ├── nav/
│   │   └── MyRoutes.kt                 // Navigation routes
│   └── di/
│       └── MyComponent.kt              // DI component
└── build.gradle.kts                    // Module dependencies
```


## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI Layer                               │
│  ┌─────────────────┐    ┌──────────────────────────────────┐    │
│  │   MyScreen.kt   │────│        Compose UI Elements       │    │
│  │                 │    │   Button(onClick = uiState.      │    │
│  │ @Composable     │    │           onButtonClicked)       │    │
│  │ operator invoke │    │   Text(text = uiState.title)     │    │
│  └─────────────────┘    └──────────────────────────────────┘    │
│           │                             │                       │
│           │ collectAsState()            │ LaunchedEffect        │
│           ▼                             ▼                       │
└───────────┼─────────────────────────────┼───────────────────────┘
            │                             │
┌───────────┼─────────────────────────────┼───────────────────────┐
│           │        ViewModel Layer      │                       │
│  ┌────────▼─────────┐          ┌────────▼────────┐              │
│  │     .state       │          │    .effects     │              │
│  │  StateFlow<      │          │ Flow<Effect>    │              │
│  │   UiState>       │          │                 │              │
│  └──────────────────┘          └─────────────────┘              │
│           ▲                             ▲                       │
│           │ updateState()               │ emitEffect()          │
│  ┌────────┼─────────────────────────────┼─────────────────────┐ │
│  │        │     UsfViewModel            │                     │ │
│  │  ┌─────▼──────────┐         ┌────────▼────────┐            │ │
│  │  │ ResultScope<   │◄────────┤     process()   │            │ │
│  │  │ State, Effect> │         │   (Event) →     │            │ │
│  │  │                │         │ State/Effects   │            │ │
│  │  └────────────────┘         └─────────────────┘            │ │
│  │           ▲                           ▲                    │ │
│  │           │ Pipeline Lifecycle        │ input(Event)       │ │
│  └───────────┼───────────────────────────┼────────────────────┘ │
│              │                           │                      │
└──────────────┼───────────────────────────┼──────────────────────┘
               │                           │
┌──────────────┼───────────────────────────┼──────────────────────┐
│              │        DI Layer           │                      │
│  ┌───────────▼─────────┐     ┌───────────▼─────────────────┐    │
│  │   CoroutineScope    │     │      MyComponent.kt         │    │
│  │ SupervisorJob() +   │     │                             │    │
│  │ Dispatchers.Main    │     │ @ContributesSubcomponent    │    │
│  │      .immediate     │     │ @Provides CoroutineScope    │    │
│  └─────────────────────┘     │ val myScreen: MyScreen      │    │
│                              └─────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Data Flow:
1. UI triggers events → viewModel.input(Event)
2. UsfViewModel.process(Event) → updateState() | emitEffect()
3. State changes → UI recomposition via collectAsState()
4. Effects → UI side effects via LaunchedEffect
```

## Real Implementation

### UsfViewModel Constructor

The `UsfViewModel` constructor accepts these parameters:

```kotlin
abstract class UsfViewModel<Event : Any, UiState : Any, Effect : Any>(
    coroutineScope: CoroutineScope,                          // Required: Injected coroutine scope
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,  // Optional: Event processing dispatcher
    inspector: UsfInspector? = null,                         // Optional: For monitoring/debugging
)
```

**Key Points:**
- **coroutineScope**: inject this via DI in your main component - don't use `viewModelScope` directly
  - USF manages this scope and pipeline directly and removes the need for an "android" dependency here
- **processingDispatcher**: Defaults to `Dispatchers.IO`, can override for testing
- **inspector**: Optional monitoring interface for debugging/analytics

### Basic ViewModel

```kotlin
@ContributesBinding(FeatureScope::class, boundType = MyViewModel::class)
class MyViewModelImpl
@Inject
constructor(
    coroutineScope: CoroutineScope,
    private val someService: SomeService,
) : MyViewModel,
    UsfViewModel<MyEvent, MyUiState, MyEffect>(
        coroutineScope = coroutineScope,
    ) {

    // Use single expression for simple initial states
    override fun initialState() = MyUiState(
        title = "Title",
        onButtonClicked = { input(MyEvent.ButtonClicked) },
        onTextChanged = { text -> input(MyEvent.TextChanged(text)) },
    )

    override suspend fun ResultScope<MyUiState, MyEffect>.process(event: MyEvent) {
        when (event) {
            is MyEvent.ButtonClicked -> {
                logcat { "[TAG] Button clicked" }  // Simple logging
                updateState { it.copy(loading = true) }
                val result = someService.doWork()
                updateState { it.copy(loading = false, data = result) }
            }
            is MyEvent.TextChanged -> {
                updateState { it.copy(text = event.text) }
            }
        }
    }

    override fun ResultScope<MyUiState, MyEffect>.onSubscribed() {
        logcat { "[TAG] Subscribed" }
        // Initialize when UI subscribes
        // updateLastRefreshTime()
    }
}
```

### UI Integration (Screen Implementation)

```kotlin
@Inject
@SingleIn(FeatureScope::class)
class MyScreen(
    private val viewModel: MyViewModel,
    private val navigator: Navigator,  // Inject Navigator directly
) {
    @Composable
    operator fun invoke() {  // Use operator invoke pattern
        val uiState by viewModel.state.collectAsState()

        // Handle one-time effects
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MyEffect.NavigateToNext -> {
                        navigator.goTo(NextRoute)  // Handle navigation internally
                    }
                    is MyEffect.ShowToast -> {
                        // Show toast
                    }
                }
            }
        }

        // UI using state
        Column {
            Text(text = uiState.title)

            Button(onClick = uiState.onButtonClicked) {
                Text("Click Me")
            }
        }
    }
}
```

### Component Registration

```kotlin
@ContributesSubcomponent(FeatureScope::class)
@SingleIn(FeatureScope::class)
interface FeatureComponent {

    // IMPORTANT: Provide CoroutineScope for USF ViewModels
    @Provides
    @SingleIn(FeatureScope::class)
    fun provideCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val myScreen: MyScreen

    @ContributesSubcomponent.Factory(AppScope::class)
    interface Factory {
        fun createFeatureComponent(): FeatureComponent

        @Provides
        @IntoSet
        fun provideFeatureEntryProvider(
            factory: Factory
        ): EntryProviderInstaller = {
            val component by lazy { factory.createFeatureComponent() }

            // Simple entry - screen handles navigation internally
            entry<MyRoute> { component.myScreen() }
        }
    }
}
```

## Key Patterns

### The ResultScope

`ResultScope` provides thread-safe operations inside `process()`:

```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    // Update state (thread-safe)
    updateState { currentState ->
        currentState.copy(loading = true)
    }

    // Emit effects (one-time actions)
    emitEffect(Effect.ShowToast("Hello"))

    // Both are automatically scoped to the pipeline
}
```

### Lifecycle Hooks

```kotlin
// Called when first UI subscriber connects
override fun ResultScope<UiState, Effect>.onSubscribed() {
    // Load initial data
    coroutineScope.launch {
        val data = repository.fetch()
        updateState { it.copy(data = data) }
    }
}

// Called when last subscriber disconnects (5-second timeout)
override fun onUnsubscribed() {
    // Cleanup resources
}
```

### Callbacks in State

⚠️ **Important:** While USF supports callbacks in state, prefer direct `viewModel.input()` calls when possible for better testability.

```kotlin
// ✅ Preferred: Direct input
Button(onClick = { viewModel.input(Event.RefreshClicked) })

// ⚠️ Use sparingly: Callbacks in state
data class UiState(
    val onRefreshClicked: () -> Unit = {}  // Harder to test
)

// When callbacks make sense:
// 1. Complex parameter transformation
val onTextChanged: (String) -> Unit  // Transforms UI input
// 2. Performance-critical (avoids recomposition)
val onScroll: (Float) -> Unit  // Called frequently
```

## Pipeline Lifecycle

USF ViewModels have smart resource management with automatic pipeline lifecycle:

### Lifecycle States

1. **Dormant** - No UI observing, no resources allocated
2. **Active** - UI subscribed, processing events and updating state
3. **Timeout** - UI gone, 5-second countdown before cleanup
4. **Cleanup** - Resources released if UI doesn't return

### Detailed Lifecycle Flow

```
┌─────────────┐    subscribe()     ┌─────────────┐
│   Dormant   │ ──────────────────▶│   Active    │
│             │                    │             │
│ • No flows  │                    │ • State     │
│ • No events │                    │ • Effects   │
│ • No scope  │                    │ • Events    │
└─────────────┘                    └─────────────┘
       ▲                                  │
       │                                  │ unsubscribe()
       │ 5s timeout                       │
       │                                  ▼
┌─────────────┐                    ┌─────────────┐
│   Cleanup   │◀───────────────────│   Timeout   │
│             │                    │             │
│ • Flows     │                    │ • Paused    │
│   closed    │                    │ • 5s timer  │
│ • Resources │                    │ • Can       │
│   released  │                    │   resume    │
└─────────────┘                    └─────────────┘
```

### Lifecycle Hooks

```kotlin
override fun ResultScope<UiState, Effect>.onSubscribed() {
    logcat { "[TAG] UI subscribed - pipeline active" }

    // Called when FIRST UI subscriber connects
    // Perfect for:
    // - Loading initial data
    // - Starting background tasks
    // - Establishing connections

    coroutineScope.launch {
        val initialData = repository.loadCachedData()
        updateState { it.copy(data = initialData) }
    }
}

override fun onUnsubscribed() {
    logcat { "[TAG] UI unsubscribed - cleanup after 5s timeout" }

    // Called when LAST UI subscriber disconnects
    // AND 5-second timeout expires
    // Perfect for:
    // - Cleanup expensive resources
    // - Close connections
    // - Cancel background work

    repository.closeConnections()
}
```

### Subscription Behavior

**Multiple Subscribers:**
```kotlin
// Multiple UI screens can observe the same ViewModel
launch { viewModel.state.collect { /* Screen A */ } }
launch { viewModel.state.collect { /* Screen B */ } }
launch { viewModel.effects.collect { /* Screen C */ } }

// Pipeline stays active until ALL subscribers stop
```

**Rapid Resubscription:**
```kotlin
// Common scenario: screen rotation, navigation back
viewModel.state.collect { /* UI active */ }
// User rotates screen or navigates away
// 5-second timer starts...
viewModel.state.collect { /* UI returns within 5s */ }
// Timer cancelled, no cleanup needed!
```

### Resource Management Examples

#### Data Loading
```kotlin
private var dataLoadJob: Job? = null

override fun ResultScope<UiState, Effect>.onSubscribed() {
    dataLoadJob = coroutineScope.launch {
        repository.startLiveDataStream()
            .collect { data ->
                updateState { it.copy(liveData = data) }
            }
    }
}

override fun onUnsubscribed() {
    dataLoadJob?.cancel()
    repository.stopLiveDataStream()
}
```

#### WebSocket Connection
```kotlin
private var webSocketConnection: WebSocket? = null

override fun ResultScope<UiState, Effect>.onSubscribed() {
    webSocketConnection = webSocketClient.connect { message ->
        updateState { it.copy(messages = it.messages + message) }
    }
}

override fun onUnsubscribed() {
    webSocketConnection?.close()
    webSocketConnection = null
}
```

#### Cleanup Prevention
```kotlin
// Prevent cleanup for critical ViewModels
override fun onUnsubscribed() {
    if (state.value.hasUnsavedChanges) {
        // Don't cleanup - keep ViewModel alive
        // Could emit effect to show "return to save" notification
        emitEffect(Effect.ShowReturnToSaveNotification)
        return
    }

    // Normal cleanup
    super.onUnsubscribed()
}
```

### Pipeline Configuration

The pipeline uses sensible defaults but can be customized:

```kotlin
class MyViewModel(
    coroutineScope: CoroutineScope,
    // Optional: Custom timeout (default: 5 seconds)
    private val cleanupTimeoutMs: Long = 5000L,
    // Optional: Custom processing dispatcher (default: Dispatchers.IO)
    processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    // Optional: Inspector for debugging/monitoring
    inspector: UsfInspector? = null,
) : UsfViewModel<Event, UiState, Effect>(
    coroutineScope = coroutineScope,
    processingDispatcher = processingDispatcher,
    inspector = inspector
) {
    // Implementation...
}
```

### Benefits of Pipeline Management

1. **Memory Efficiency**: Resources are automatically cleaned up when UI is gone
2. **Battery Optimization**: Background work stops when UI isn't active
3. **Fast Recovery**: Quick resubscription doesn't restart expensive operations
4. **Developer Friendly**: Lifecycle management is mostly automatic
5. **Predictable**: Clear hooks for initialization and cleanup

### Testing Pipeline Lifecycle

```kotlin
@Test
fun `pipeline activates on subscription and cleans up after timeout`() = runTest {
    val viewModel = MyViewModelImpl(backgroundScope)

    // Start observing - triggers activation
    val job = backgroundScope.launch {
        viewModel.state.collect { /* consume */ }
    }
    runCurrent()

    // Verify pipeline is active
    verify(exactly = 1) { mockRepository.startLiveData() }

    // Stop observing - starts cleanup timer
    job.cancel()

    // Advance past timeout (5 seconds)
    advanceTimeBy(6000)
    runCurrent()

    // Verify cleanup occurred
    verify(exactly = 1) { mockRepository.stopLiveData() }
}
```

This automatic lifecycle management is a key advantage of USF - ViewModels are efficient and developers don't need to manually manage subscriptions.

## Advanced: Plugin Architecture

For complex features with reusable logic, USF supports plugin composition.

**When to use plugins:**
- Multiple ViewModels need the same feature (search, pagination, validation)
- Complex ViewModels need modular breakdown
- Isolated testing of feature components

**Example:**
```kotlin
class ComplexViewModel : UsfViewModel<Event, UiState, Effect>() {
    init {
        register(SearchPlugin(), /* adapters */)
        register(PaginationPlugin(), /* adapters */)
    }
}
```

### Effect-to-Event Transformation

Advanced USF feature allowing plugins to trigger parent ViewModel events through their effects:

```kotlin
// Plugin emits effect
emitEffect(PluginEffect.TriggerParentRefresh)

// Adapter transforms effect to parent event
transformEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.TriggerParentRefresh -> Event.RefreshData
        is PluginEffect.NavigateToDetails -> Event.NavigateToScreen("details")
        else -> null
    }
}
```

**Use cases:**
- **Plugin Feedback Loops**: Plugins trigger parent actions based on their state
- **Navigation Control**: Plugins initiate navigation through parent ViewModel
- **State Coordination**: Plugins reset or update parent state when needed
- **Error Handling**: Plugins trigger parent error handling flows
- **Complex Workflows**: Multi-step processes coordinated between plugins and parent

This enables sophisticated plugin coordination where plugins can influence the parent ViewModel's behavior beyond just state and effects.

See [USF Plugin Architecture](#usf-plugin-architecture) for complete implementation details.

## Common Patterns

### Loading States
```kotlin
override suspend fun process(event: Event) {
    when (event) {
        is Event.Load -> {
            updateState { it.copy(loading = true) }
            try {
                val data = repository.fetch()
                updateState { it.copy(loading = false, data = data) }
            } catch (e: Exception) {
                updateState { it.copy(loading = false, error = e.message) }
                emitEffect(Effect.ShowError(e.message))
            }
        }
    }
}
```

### Error Handling Patterns

#### Basic Error Handling
```kotlin
data class UiState(
    val data: List<Item> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val hasError: Boolean = false,
)

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadData -> {
            updateState {
                it.copy(loading = true, errorMessage = null, hasError = false)
            }

            try {
                val data = repository.fetchData()
                updateState {
                    it.copy(loading = false, data = data)
                }
            } catch (e: Exception) {
                logcat { "[TAG] Error loading data: ${e.message}" }
                updateState {
                    it.copy(
                        loading = false,
                        hasError = true,
                        errorMessage = when (e) {
                            is NetworkException -> "Network error. Check your connection."
                            is AuthException -> "Authentication failed. Please log in again."
                            else -> "Something went wrong. Please try again."
                        }
                    )
                }
                emitEffect(Effect.ShowErrorToast(e.message))
            }
        }
    }
}
```

#### Retry Logic with Exponential Backoff
```kotlin
private suspend fun ResultScope<UiState, Effect>.performWithRetry(
    maxRetries: Int = 3,
    operation: suspend () -> Unit
) {
    var attempts = 0
    var lastException: Exception? = null

    while (attempts < maxRetries) {
        try {
            operation()
            return // Success
        } catch (e: Exception) {
            lastException = e
            attempts++

            if (attempts < maxRetries) {
                val delayMs = (1000L * (1 shl attempts)).coerceAtMost(10000L)
                updateState {
                    it.copy(retryMessage = "Retrying in ${delayMs/1000}s... (${attempts}/$maxRetries)")
                }
                delay(delayMs)
            }
        }
    }

    // All retries failed
    throw lastException ?: Exception("Operation failed after $maxRetries attempts")
}

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadCriticalData -> {
            updateState { it.copy(loading = true, errorMessage = null) }

            try {
                performWithRetry(maxRetries = 3) {
                    val data = repository.fetchCriticalData()
                    updateState { it.copy(data = data, loading = false) }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        loading = false,
                        errorMessage = "Failed to load data after multiple attempts",
                        retryMessage = null
                    )
                }
                emitEffect(Effect.ShowRetryDialog)
            }
        }
    }
}
```

#### Cancellation Handling
```kotlin
private var loadJob: Job? = null

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.StartLoad -> {
            // Cancel previous operation
            loadJob?.cancel()

            updateState { it.copy(loading = true) }

            loadJob = coroutineScope.launch {
                try {
                    val data = repository.fetchData()
                    updateState { it.copy(loading = false, data = data) }
                } catch (e: CancellationException) {
                    // Expected cancellation - don't treat as error
                    logcat { "[TAG] Load operation cancelled" }
                    updateState { it.copy(loading = false) }
                } catch (e: Exception) {
                    updateState {
                        it.copy(loading = false, errorMessage = e.message)
                    }
                }
            }
        }

        is Event.CancelLoad -> {
            loadJob?.cancel()
            loadJob = null
            updateState { it.copy(loading = false) }
        }
    }
}
```

#### Error Recovery States
```kotlin
data class UiState(
    val content: List<Item> = emptyList(),
    val loading: Boolean = false,
    val error: ErrorState? = null,
)

sealed interface ErrorState {
    data class NetworkError(val canRetry: Boolean = true) : ErrorState
    data class AuthError(val needsReauth: Boolean = true) : ErrorState
    data class ValidationError(val fields: List<String>) : ErrorState
    data class GenericError(val message: String, val canRetry: Boolean = false) : ErrorState
}

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.RetryOperation -> {
            when (val error = state.value.error) {
                is ErrorState.NetworkError -> {
                    if (error.canRetry) {
                        input(Event.LoadData) // Retry the original operation
                    }
                }
                is ErrorState.AuthError -> {
                    emitEffect(Effect.NavigateToLogin)
                }
                else -> {
                    emitEffect(Effect.ShowToast("Cannot retry this operation"))
                }
            }
        }

        is Event.HandleError -> {
            val errorState = when (event.exception) {
                is IOException -> ErrorState.NetworkError(canRetry = true)
                is HttpException -> when (event.exception.code()) {
                    401, 403 -> ErrorState.AuthError(needsReauth = true)
                    in 500..599 -> ErrorState.NetworkError(canRetry = true)
                    else -> ErrorState.GenericError(
                        message = "Server error: ${event.exception.code()}",
                        canRetry = false
                    )
                }
                is ValidationException -> ErrorState.ValidationError(
                    fields = event.exception.fields
                )
                else -> ErrorState.GenericError(
                    message = event.exception.message ?: "Unknown error",
                    canRetry = false
                )
            }

            updateState { it.copy(error = errorState, loading = false) }
        }
    }
}
```

#### Race Condition Prevention
```kotlin
private data class OperationState(
    val operationId: String,
    val job: Job
)

private var currentOperation: OperationState? = null

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.StartOperation -> {
            val operationId = UUID.randomUUID().toString()

            // Cancel previous operation
            currentOperation?.job?.cancel()

            val job = coroutineScope.launch {
                try {
                    updateState { it.copy(loading = true) }
                    val result = repository.performOperation(event.data)

                    // Check if this operation is still current
                    if (currentOperation?.operationId == operationId) {
                        updateState {
                            it.copy(loading = false, result = result)
                        }
                    }
                } catch (e: CancellationException) {
                    // Expected - operation was superseded
                } catch (e: Exception) {
                    if (currentOperation?.operationId == operationId) {
                        updateState {
                            it.copy(loading = false, error = e.message)
                        }
                    }
                } finally {
                    if (currentOperation?.operationId == operationId) {
                        currentOperation = null
                    }
                }
            }

            currentOperation = OperationState(operationId, job)
        }
    }
}
```

### Form Validation
```kotlin
data class FormState(
    val email: String = "",
    val isValid: Boolean = false,
)

override suspend fun process(event: Event) {
    when (event) {
        is Event.EmailChanged -> {
            val isValid = event.email.contains("@")
            updateState {
                it.copy(email = event.email, isValid = isValid)
            }
        }
        is Event.Submit -> {
            if (state.value.isValid) {
                submitForm()
            } else {
                emitEffect(Effect.ShowError("Invalid email"))
            }
        }
    }
}
```

### Debounced Operations
```kotlin
private var searchJob: Job? = null

override suspend fun process(event: Event) {
    when (event) {
        is Event.SearchTextChanged -> {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(300)  // Debounce
                val results = repository.search(event.text)
                updateState { it.copy(results = results) }
            }
        }
    }
}
```

## Testing

```kotlin
@Test
fun `refresh updates time and shows toast`() = runTest {
    val viewModel = HomeViewModelImpl(
        coroutineScope = TestScope()
    )

    // Collect states and effects
    val states = mutableListOf<HomeUiState>()
    val effects = mutableListOf<HomeEffect>()

    launch { viewModel.state.toList(states) }
    launch { viewModel.effects.toList(effects) }

    // Trigger event
    viewModel.input(HomeEvent.RefreshClicked)
    advanceUntilIdle()

    // Verify state updated
    assertNotNull(states.last().lastRefreshTime)

    // Verify effect emitted
    assertTrue(effects.any { it is HomeEffect.ShowToast })
}
```

## Migration from Standard ViewModel

```kotlin
// Before: Standard ViewModel
class OldViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun onButtonClick() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val data = repository.fetch()
            _state.value = _state.value.copy(loading = false, data = data)
        }
    }
}

// After: USF ViewModel
class NewViewModel(
    coroutineScope: CoroutineScope  // Injected, not viewModelScope
) : UsfViewModel<Event, UiState, Effect>(coroutineScope) {

    override fun initialState() = UiState()

    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        when (event) {
            is Event.ButtonClick -> {
                updateState { it.copy(loading = true) }
                val data = repository.fetch()
                updateState { it.copy(loading = false, data = data) }
            }
        }
    }
}
```

## Code Style Guidelines

### Preferred Patterns

```kotlin
// ✅ Single expression for simple functions
override fun initialState() = MyUiState()

// ✅ Inject Navigator directly into screens
class MyScreen(
    private val viewModel: MyViewModel,
    private val navigator: Navigator,
)

// ✅ Simple logging without complex tags
logcat { "[TAG] Message" }

// ✅ Direct dependency injection (no Bindings classes)
@Inject
constructor(
    private val service: MyService,
    @Named("appName") private val appName: String,
)

// ✅ Abbreviated names for long screen names
// SettingsAScreen → SAEvent, SAUiState, SAEffect
// SettingsBScreen → SBEvent, SBUiState, SBEffect
```

### Avoid

```kotlin
// ❌ Unnecessary return statements
override fun initialState(): MyUiState {
    return MyUiState()
}

// ❌ Passing navigation callbacks as parameters
class MyScreen(
    @Assisted navToNext: () -> Unit
)

// ❌ Complex logging tags
logcat("MyScreen") { "xxx injected value → $value" }

// ❌ Grouping dependencies in Bindings classes
class FeatureBindings(val service: Service, val repo: Repo)
```

## Key Benefits

1. **Predictable** - Events always flow the same way
2. **Testable** - Pure functions, clear boundaries
3. **Efficient** - Smart lifecycle management
4. **Safe** - Thread-safe state updates
5. **Clean** - Less boilerplate than standard ViewModels

## Real-World Examples

### E-Commerce Product Listing

A comprehensive example showing search, filtering, pagination, and error handling:

```kotlin
// Product domain models
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val isInStock: Boolean,
)

data class SearchFilters(
    val categories: Set<String> = emptySet(),
    val priceRange: ClosedFloatingPointRange<Double>? = null,
    val inStockOnly: Boolean = false,
)

// Events - All user interactions
sealed interface ProductListEvent {
    data object InitialLoad : ProductListEvent
    data object RefreshPull : ProductListEvent
    data object LoadMore : ProductListEvent
    data class SearchQueryChanged(val query: String) : ProductListEvent
    data class FiltersChanged(val filters: SearchFilters) : ProductListEvent
    data object ClearFilters : ProductListEvent
    data class ProductClicked(val product: Product) : ProductListEvent
    data object RetryLoad : ProductListEvent
    data object ClearError : ProductListEvent
}

// State - Complete UI representation
data class ProductListUiState(
    // Core data
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val filters: SearchFilters = SearchFilters(),

    // Loading states
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSearching: Boolean = false,

    // Pagination
    val hasMorePages: Boolean = true,
    val currentPage: Int = 1,

    // Error handling
    val error: ProductListError? = null,
    val canRetry: Boolean = false,

    // UI state
    val searchSuggestions: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList(),

    // Callbacks (use sparingly - prefer direct viewModel.input() calls)
    val onSearchChanged: (String) -> Unit = {},
    val onFiltersChanged: (SearchFilters) -> Unit = {},
    val onProductClicked: (Product) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

// Effects - One-time side effects
sealed interface ProductListEffect {
    data class NavigateToProduct(val productId: String) : ProductListEffect
    data class NavigateToFilters(val currentFilters: SearchFilters) : ProductListEffect
    data class ShowToast(val message: String) : ProductListEffect
    data object ShowFilterBottomSheet : ProductListEffect
    data class TrackSearch(val query: String, val resultCount: Int) : ProductListEffect
}

// Error types for better error handling
sealed interface ProductListError {
    data object NetworkError : ProductListError
    data object SearchTimeout : ProductListError
    data class ApiError(val code: Int, val message: String) : ProductListError
    data class UnknownError(val message: String) : ProductListError
}

// ViewModel Implementation
@ContributesBinding(ProductScope::class, boundType = ProductListViewModel::class)
@SingleIn(ProductScope::class)
@Inject
class ProductListViewModelImpl(
    coroutineScope: CoroutineScope,
    private val productRepository: ProductRepository,
    private val analyticsService: AnalyticsService,
) : ProductListViewModel,
    UsfViewModel<ProductListEvent, ProductListUiState, ProductListEffect>(
        coroutineScope = coroutineScope,
    ) {

    // Debounced search job
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun initialState() = ProductListUiState(
        onSearchChanged = { query -> input(ProductListEvent.SearchQueryChanged(query)) },
        onFiltersChanged = { filters -> input(ProductListEvent.FiltersChanged(filters)) },
        onProductClicked = { product -> input(ProductListEvent.ProductClicked(product)) },
        onRefresh = { input(ProductListEvent.RefreshPull) },
        onLoadMore = { input(ProductListEvent.LoadMore) },
        onRetry = { input(ProductListEvent.RetryLoad) },
    )

    override suspend fun ResultScope<ProductListUiState, ProductListEffect>.process(
        event: ProductListEvent
    ) {
        when (event) {
            is ProductListEvent.InitialLoad -> handleInitialLoad()

            is ProductListEvent.RefreshPull -> handleRefresh()

            is ProductListEvent.LoadMore -> handleLoadMore()

            is ProductListEvent.SearchQueryChanged -> handleSearchQuery(event.query)

            is ProductListEvent.FiltersChanged -> handleFiltersChanged(event.filters)

            is ProductListEvent.ClearFilters -> {
                updateState { it.copy(filters = SearchFilters()) }
                performSearch()
            }

            is ProductListEvent.ProductClicked -> {
                emitEffect(ProductListEffect.NavigateToProduct(event.product.id))
                analyticsService.trackProductClick(event.product.id)
            }

            is ProductListEvent.RetryLoad -> handleRetry()

            is ProductListEvent.ClearError -> {
                updateState { it.copy(error = null, canRetry = false) }
            }
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleInitialLoad() {
        updateState { it.copy(isInitialLoading = true, error = null) }

        try {
            val (products, categories) = loadProductsAndCategories(page = 1)

            updateState {
                it.copy(
                    isInitialLoading = false,
                    products = products,
                    availableCategories = categories,
                    currentPage = 1,
                    hasMorePages = products.size >= PAGE_SIZE
                )
            }
        } catch (e: Exception) {
            handleError(e, isInitialLoad = true)
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleRefresh() {
        updateState { it.copy(isRefreshing = true, error = null) }

        try {
            val (products, categories) = loadProductsAndCategories(page = 1)

            updateState {
                it.copy(
                    isRefreshing = false,
                    products = products,
                    availableCategories = categories,
                    currentPage = 1,
                    hasMorePages = products.size >= PAGE_SIZE
                )
            }

            emitEffect(ProductListEffect.ShowToast("Products refreshed"))
        } catch (e: Exception) {
            handleError(e, isRefresh = true)
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleLoadMore() {
        val currentState = state.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) {
            return
        }

        // Cancel previous load more operation
        loadMoreJob?.cancel()

        updateState { it.copy(isLoadingMore = true) }

        loadMoreJob = coroutineScope.launch {
            try {
                val nextPage = currentState.currentPage + 1
                val (newProducts, _) = loadProductsAndCategories(page = nextPage)

                updateState {
                    it.copy(
                        isLoadingMore = false,
                        products = it.products + newProducts,
                        currentPage = nextPage,
                        hasMorePages = newProducts.size >= PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                updateState { it.copy(isLoadingMore = false) }
                emitEffect(ProductListEffect.ShowToast("Failed to load more products"))
            }
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleSearchQuery(
        query: String
    ) {
        updateState { it.copy(searchQuery = query) }

        // Cancel previous search
        searchJob?.cancel()

        if (query.isBlank()) {
            // Clear search - reload all products
            performSearch()
            return
        }

        // Debounce search
        searchJob = coroutineScope.launch {
            delay(300) // Debounce delay
            performSearch()
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleFiltersChanged(
        filters: SearchFilters
    ) {
        updateState { it.copy(filters = filters) }
        performSearch()
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.performSearch() {
        val currentState = state.value
        updateState { it.copy(isSearching = true, error = null) }

        try {
            val (products, _) = loadProductsAndCategories(
                page = 1,
                query = currentState.searchQuery.takeIf { it.isNotBlank() },
                filters = currentState.filters
            )

            updateState {
                it.copy(
                    isSearching = false,
                    products = products,
                    currentPage = 1,
                    hasMorePages = products.size >= PAGE_SIZE
                )
            }

            // Track search analytics
            if (currentState.searchQuery.isNotBlank()) {
                emitEffect(
                    ProductListEffect.TrackSearch(
                        query = currentState.searchQuery,
                        resultCount = products.size
                    )
                )
            }
        } catch (e: Exception) {
            handleError(e, isSearch = true)
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleRetry() {
        when {
            state.value.products.isEmpty() -> handleInitialLoad()
            else -> performSearch()
        }
    }

    private suspend fun ResultScope<ProductListUiState, ProductListEffect>.handleError(
        exception: Exception,
        isInitialLoad: Boolean = false,
        isRefresh: Boolean = false,
        isSearch: Boolean = false
    ) {
        logcat { "[PLS] Error occurred: ${exception.message}" }

        val error = when (exception) {
            is IOException -> ProductListError.NetworkError
            is HttpException -> when (exception.code()) {
                408 -> ProductListError.SearchTimeout
                in 400..499 -> ProductListError.ApiError(exception.code(), "Client error")
                in 500..599 -> ProductListError.ApiError(exception.code(), "Server error")
                else -> ProductListError.UnknownError("HTTP ${exception.code()}")
            }
            else -> ProductListError.UnknownError(exception.message ?: "Unknown error")
        }

        updateState {
            it.copy(
                isInitialLoading = false,
                isRefreshing = false,
                isSearching = false,
                error = error,
                canRetry = true
            )
        }

        val message = when (error) {
            is ProductListError.NetworkError -> "Check your internet connection"
            is ProductListError.SearchTimeout -> "Search timed out. Please try again"
            is ProductListError.ApiError -> "Service temporarily unavailable"
            is ProductListError.UnknownError -> "Something went wrong"
        }

        emitEffect(ProductListEffect.ShowToast(message))
    }

    private suspend fun loadProductsAndCategories(
        page: Int,
        query: String? = null,
        filters: SearchFilters = SearchFilters()
    ): Pair<List<Product>, List<String>> {
        // Simulate network call
        return productRepository.searchProducts(
            query = query,
            categories = filters.categories,
            priceRange = filters.priceRange,
            inStockOnly = filters.inStockOnly,
            page = page,
            pageSize = PAGE_SIZE
        ) to productRepository.getAvailableCategories()
    }

    override fun ResultScope<ProductListUiState, ProductListEffect>.onSubscribed() {
        logcat { "[PLS] Product list screen subscribed" }

        // Auto-load on first subscription
        if (state.value.products.isEmpty()) {
            coroutineScope.launch {
                input(ProductListEvent.InitialLoad)
            }
        }
    }

    override fun onUnsubscribed() {
        logcat { "[PLS] Product list screen unsubscribed" }

        // Cancel ongoing operations
        searchJob?.cancel()
        loadMoreJob?.cancel()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
```

### UI Implementation for Complex Example

```kotlin
@Inject
@SingleIn(ProductScope::class)
class ProductListScreen(
    private val viewModel: ProductListViewModel,
    private val navigator: Navigator,
) {
    @Composable
    operator fun invoke() {
        val uiState by viewModel.state.collectAsState()

        // Handle effects
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ProductListEffect.NavigateToProduct -> {
                        navigator.goTo(ProductDetailRoute(effect.productId))
                    }
                    is ProductListEffect.NavigateToFilters -> {
                        navigator.goTo(FiltersRoute(effect.currentFilters))
                    }
                    is ProductListEffect.ShowToast -> {
                        // Show toast implementation
                    }
                    is ProductListEffect.ShowFilterBottomSheet -> {
                        // Show bottom sheet implementation
                    }
                    is ProductListEffect.TrackSearch -> {
                        // Analytics tracking handled in ViewModel
                    }
                }
            }
        }

        ProductListContent(
            uiState = uiState,
            onSearchChanged = uiState.onSearchChanged,
            onProductClicked = uiState.onProductClicked,
            onRefresh = uiState.onRefresh,
            onLoadMore = uiState.onLoadMore,
            onRetry = uiState.onRetry,
            onFiltersClicked = {
                viewModel.input(ProductListEvent.NavigateToFilters)
            }
        )
    }
}

@Composable
private fun ProductListContent(
    uiState: ProductListUiState,
    onSearchChanged: (String) -> Unit,
    onProductClicked: (Product) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onFiltersClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar
        item {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = onSearchChanged,
                isSearching = uiState.isSearching,
                onFiltersClicked = onFiltersClicked
            )
        }

        // Error state
        if (uiState.error != null) {
            item {
                ErrorCard(
                    error = uiState.error,
                    canRetry = uiState.canRetry,
                    onRetry = onRetry
                )
            }
        }

        // Loading state
        if (uiState.isInitialLoading) {
            items(6) { LoadingProductCard() }
        } else {
            // Product list
            items(uiState.products) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClicked(product) }
                )
            }

            // Load more indicator
            if (uiState.isLoadingMore && uiState.hasMorePages) {
                item {
                    LoadingIndicator()
                    LaunchedEffect(Unit) { onLoadMore() }
                }
            }
        }
    }

    // Pull-to-refresh
    if (uiState.isRefreshing) {
        PullRefreshIndicator(refreshing = true, onRefresh = onRefresh)
    }
}
```

This example demonstrates:
- **Complex State Management**: Multiple loading states, pagination, error handling
- **Debounced Operations**: Search with 300ms debounce
- **Async Coordination**: Cancellation of previous operations
- **Error Recovery**: Retry logic with different error types
- **Resource Management**: Lifecycle-aware cleanup
- **Analytics Integration**: Event tracking
- **Performance Optimization**: Pagination and efficient loading

USF makes complex ViewModels manageable and simple ViewModels trivial.

# USF Plugin Architecture

> **Note**: This is an advanced USF feature. Read [USF (Unidirectional State Flow) Architecture](#usf-unidirectional-state-flow-architecture) first for core USF concepts.

## Quick Overview

USF Plugins let you compose complex ViewModels from reusable components. Think "feature mixins" for ViewModels.

**Without Plugins:**
```kotlin
class ProductViewModel : UsfViewModel<Event, UiState, Effect>() {
    // All logic in one place - gets messy
    override suspend fun process(event: Event) {
        when (event) {
            is SearchEvent -> handleSearch()
            is PaginationEvent -> handlePagination()
            is FilterEvent -> handleFilter()
            // Growing complexity...
        }
    }
}
```

**With Plugins:**
```kotlin
class ProductViewModel : UsfViewModel<Event, UiState, Effect>() {
    init {
        // Compose from reusable plugins
        register(SearchPlugin())
        register(PaginationPlugin())
        register(FilterPlugin())
    }
}
```

## Core Concepts

### Plugin Interface

Plugins use the same `ResultScope` pattern as standard USF ViewModels:

```kotlin
interface UsfPluginInterface<Event : Any, UiState : Any, Effect : Any> {
    suspend fun ResultScope<UiState, Effect>.process(event: Event)
    fun ResultScope<UiState, Effect>.onSubscribed() {}
    fun onUnsubscribed() {}
}
```

Base implementation with optional internal state:

```kotlin
abstract class UsfPlugin<Event : Any, UiState : Any, Effect : Any> :
    UsfPluginInterface<Event, UiState, Effect> {
    protected val internalState = InternalStateHolder(initialInternalState())
}
```

## When to Use Plugins

**When to use plugins:**
- Multiple ViewModels need the same feature (search, pagination, validation)
- Complex ViewModels need modular breakdown
- Isolated testing of feature components

### ✅ Perfect For

1. **Reusable Logic Across ViewModels**
   - Search functionality
   - Pagination
   - Filtering/sorting
   - Form validation

2. **Complex Features**
   - E-commerce listings (search + filters + pagination)
   - Chat screens (messaging + typing + presence)
   - Media players (playback + playlist + controls)

3. **Isolated Testing**
   - Test search independently
   - Mock one plugin while testing another

### ❌ Not Needed For

- Simple CRUD ViewModels
- One-off logic
- Tightly coupled features

### Benefits
1. Separation of Concerns: Each plugin handles a specific domain of functionality
2. Testability: Plugins can be tested independently with their own test suites
3. Reusability: Plugins can be used across multiple ViewModels and features
4. Composition: Multiple plugins can be composed together to build complex behavior
5. Maintainability: Smaller, focused components are easier to understand and maintain
6. Scalability: Large ViewModels can be broken down into manageable pieces

## Implementation Example

### Creating a Search Plugin

```kotlin
class SearchPlugin<T> : UsfPlugin<SearchEvent, SearchState<T>, SearchEffect>() {

    data class SearchState<T>(
        val query: String = "",
        val results: List<T> = emptyList(),
        val isSearching: Boolean = false,
    )

    sealed interface SearchEvent {
        data class QueryChanged(val query: String) : SearchEvent
        data object Clear : SearchEvent
    }

    private var searchJob: Job? = null

    override suspend fun ResultScope<SearchState<T>, SearchEffect>.process(
        event: SearchEvent
    ) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                searchJob?.cancel()
                updateState { it.copy(query = event.query, isSearching = true) }

                searchJob = coroutineScope.launch {
                    delay(300)  // Debounce
                    val results = performSearch(event.query)
                    updateState {
                        it.copy(results = results, isSearching = false)
                    }
                }
            }
            is SearchEvent.Clear -> {
                searchJob?.cancel()
                updateState { SearchState() }
            }
        }
    }

    abstract suspend fun performSearch(query: String): List<T>
}
```

### Using Plugin in ViewModel

```kotlin
class ProductListViewModel(
    coroutineScope: CoroutineScope
) : UsfViewModel<Event, UiState, Effect>(coroutineScope) {

    init {
        // Register search plugin with adapters
        register(
            plugin = ProductSearchPlugin(),
            // Map ViewModel events to plugin events
            mapEvent = { event: Event ->
                when (event) {
                    is Event.SearchQueryChanged ->
                        SearchEvent.QueryChanged(event.query)
                    is Event.ClearSearch ->
                        SearchEvent.Clear
                    else -> null  // Not for this plugin
                }
            },
            // Merge plugin state into ViewModel state
            applyState = { vmState: UiState, searchState: SearchState<Product> ->
                vmState.copy(
                    products = searchState.results,
                    searchQuery = searchState.query,
                    isLoading = searchState.isSearching
                )
            },
            // Convert plugin effects to ViewModel effects
            mapEffect = { searchEffect: SearchEffect ->
                when (searchEffect) {
                    is SearchEffect.Error -> Effect.ShowError(searchEffect.message)
                    else -> null
                }
            },
            // NEW: Transform plugin effects to parent events
            transformEffect = { searchEffect: SearchEffect ->
                when (searchEffect) {
                    is SearchEffect.TriggerRefresh -> Event.RefreshProducts
                    is SearchEffect.RequestClearAll -> Event.ClearAllData
                    else -> null
                }
            }
        )
    }

    // ViewModel still handles its own events
    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        when (event) {
            is Event.RefreshProducts -> loadProducts()
            is Event.ClearAllData -> {
                updateState { it.copy(products = emptyList()) }
            }
            // Search events handled by plugin
        }
    }
}
```

## Plugin Adapters Explained

Adapters translate between plugin and ViewModel types:

### Event Adapter (mapEvent)
```kotlin
mapEvent = { vmEvent ->
    when (vmEvent) {
        is VmEvent.Search -> PluginEvent.Search(vmEvent.query)
        else -> null  // Not for this plugin
    }
}
```

### State Adapter (applyState)
```kotlin
applyState = { vmState, pluginState ->
    vmState.copy(
        searchResults = pluginState.results,
        isSearching = pluginState.isLoading
    )
}
```

### Effect Adapter (mapEffect)
```kotlin
mapEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.Error -> VmEffect.ShowToast(pluginEffect.message)
        else -> null
    }
}
```

### Effect-to-Event Adapter (transformEffect) - NEW!
```kotlin
transformEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.TriggerParentRefresh -> VmEvent.RefreshData
        is PluginEffect.NavigateToDetails -> VmEvent.NavigateToScreen("details")
        else -> null  // Not transformed to event
    }
}
```

## Internal State

Plugins can maintain hidden state:

```kotlin
class PaginationPlugin<T> : UsfPlugin<Event, State<T>, Effect>() {

    // Not exposed to ViewModel
    data class InternalState(
        val currentPage: Int = 1,
        val hasMore: Boolean = true,
    )

    override fun initialInternalState() = InternalState()

    override suspend fun process(event: Event) {
        when (event) {
            is Event.LoadMore -> {
                val internal = internalState.get<InternalState>()
                if (!internal.hasMore) return

                val nextPage = internal.currentPage + 1
                val items = loadPage(nextPage)

                internalState.update {
                    it.copy(currentPage = nextPage, hasMore = items.isNotEmpty())
                }

                updateState { it.copy(items = it.items + items) }
            }
        }
    }
}
```

## Testing Plugins

Plugins are independently testable using the same patterns as standard USF ViewModels.

### Basic Plugin Testing

```kotlin
class SearchPluginTest {

    @Test
    fun `search plugin handles query changes with debounce`() = runTest {
        val plugin = SearchPlugin<Product> { query ->
            productRepository.search(query)
        }

        val mockScope = mockk<ResultScope<SearchState<Product>, SearchEffect>>()
        val states = mutableListOf<SearchState<Product>>()

        every { mockScope.updateState(any()) } answers {
            val updater = arg<(SearchState<Product>) -> SearchState<Product>>(0)
            states.add(updater(states.lastOrNull() ?: SearchState()))
        }

        // Rapid queries
        plugin.run { mockScope.process(SearchEvent.QueryChanged("a")) }
        plugin.run { mockScope.process(SearchEvent.QueryChanged("ab")) }
        plugin.run { mockScope.process(SearchEvent.QueryChanged("abc")) }

        // Advance past debounce
        advanceTimeBy(400)
        runCurrent()

        // Only final query should have triggered search
        assertThat(states.last().query).isEqualTo("abc")
        coVerify(exactly = 1) { productRepository.search("abc") }
    }
}
```

### Testing Plugin Integration

```kotlin
@Test
fun `view model with search plugin integrates correctly`() = runTest {
    val searchPlugin = mockk<SearchPlugin<Product>>()
    val viewModel = ProductViewModelImpl(
        coroutineScope = backgroundScope,
        searchPlugin = searchPlugin
    )

    coEvery {
        searchPlugin.process(any<SearchEvent>())
    } coAnswers {
        // Mock plugin updating its internal state
        SearchState(query = "test", results = listOf(Product("test")))
    }

    val states = mutableListOf<ProductUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    // Act
    viewModel.input(ProductEvent.SearchQueryChanged("test"))
    runCurrent()

    // Assert plugin was called
    coVerify { searchPlugin.process(SearchEvent.QueryChanged("test")) }

    // Assert ViewModel state includes plugin data
    assertThat(states.last().searchResults).hasSize(1)
    assertThat(states.last().searchResults.first().name).isEqualTo("test")
}
```

For comprehensive testing patterns and examples, see [USF Testing Guide](#usf-testing-guide).

## Common Plugin Patterns

### Form Validation
```kotlin
class ValidationPlugin : UsfPlugin<Event, State, Effect>() {
    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        when (event) {
            is Event.ValidateField -> {
                val isValid = validateField(event.field, event.value)
                if (!isValid) {
                    emitEffect(Effect.TriggerParentErrorDisplay) // Effect-to-event
                }
            }
        }
    }
}
```

### Network Retry
```kotlin
class RetryPlugin : UsfPlugin<Event, State, Effect>() {
    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        when (event) {
            is Event.NetworkError -> {
                delay(getBackoffDelay())
                emitEffect(Effect.TriggerParentRetry) // Effect-to-event transformation
            }
        }
    }
}
```

### Analytics
```kotlin
class AnalyticsPlugin : UsfPlugin<Event, State, Effect>() {
    override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
        // Auto-track all events
        analyticsService.track(event)

        // Trigger navigation based on analytics
        if (shouldTriggerOnboarding(event)) {
            emitEffect(Effect.TriggerOnboardingFlow) // Effect-to-event
        }
    }
}
```

## Current App Status

The app has USF Plugin support but **currently uses standard USF ViewModels** for simplicity:

- HomeViewModel - Standard USF
- SettingsViewModel - Standard USF
- DiscoverViewModel - Standard USF

Consider plugins when:
1. Multiple screens need search
2. Pagination logic needs sharing
3. Complex features need isolation

## Migration Path

```kotlin
// Before: Monolithic ViewModel
class MyViewModel : UsfViewModel<Event, UiState, Effect>() {
    override suspend fun process(event: Event) {
        // 200 lines of mixed logic
    }
}

// After: Composed from plugins
class MyViewModel : UsfViewModel<Event, UiState, Effect>() {
    init {
        register(SearchPlugin(), /* adapters */)
        register(FilterPlugin(), /* adapters */)
    }

    override suspend fun process(event: Event) {
        // Only ViewModel-specific logic
    }
}
```

## Best Practices

### DO
- Keep plugins focused (one responsibility)
- Test plugins independently
- Document plugin contracts
- Use for genuinely reusable logic

### DON'T
- Create mega-plugins (prefer composition)
- Use for one-off logic
- Forget error handling
- Over-engineer simple ViewModels

## Key Benefits

1. **Reusability** - Write once, use everywhere
2. **Testability** - Test in isolation
3. **Maintainability** - Smaller components
4. **Flexibility** - Mix and match features
5. **Separation** - Clear boundaries
6. **Effect-to-Event Flow** - Plugins can trigger parent actions
7. **Complex Workflows** - Enable sophisticated plugin coordination

## Effect-to-Event Use Cases

### Navigation Control
```kotlin
// Plugin can trigger parent navigation
transformEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.NavigateToDetails -> Event.NavigateToScreen("details")
        is PluginEffect.GoBack -> Event.NavigateBack
        else -> null
    }
}
```

### State Coordination
```kotlin
// Plugin can reset parent state
transformEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.InvalidateData -> Event.ClearAllData
        is PluginEffect.RequestRefresh -> Event.RefreshData
        else -> null
    }
}
```

### Error Handling
```kotlin
// Plugin can trigger parent error handling
transformEffect = { pluginEffect ->
    when (pluginEffect) {
        is PluginEffect.CriticalError -> Event.ShowGlobalError(pluginEffect.message)
        is PluginEffect.AuthenticationFailed -> Event.LogoutUser
        else -> null
    }
}
```

USF Plugins are powerful but optional - use them when complexity demands it.

# USF Quickstart Guide

> **Start here** if you're new to USF. This guide walks you through creating your first USF feature from scratch.

## What You'll Build

A simple Counter feature that demonstrates the core USF concepts:
- User can increment/decrement a counter
- Shows loading state during simulated async operations
- Displays toast messages as side effects

## Prerequisites

- Read [USF (Unidirectional State Flow) Architecture](#usf-unidirectional-state-flow-architecture) for conceptual overview
- Understanding of Kotlin, Coroutines, and Jetpack Compose
- Familiarity with Dependency Injection concepts

## Step 1: Define Your Event, State, and Effect Types

Create the data contracts for your feature:

```kotlin
// CounterEvent.kt - What can happen (user actions)
sealed interface CounterEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
    data object Reset : CounterEvent
    data object AsyncIncrement : CounterEvent  // Simulates async operation
}

// CounterUiState.kt - What the UI shows
data class CounterUiState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Callbacks - use sparingly, prefer direct viewModel.input() calls
    val onIncrement: () -> Unit = {},
    val onDecrement: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onAsyncIncrement: () -> Unit = {},
)

// CounterEffect.kt - One-time side effects
sealed interface CounterEffect {
    data class ShowToast(val message: String) : CounterEffect
    data object NavigateToSettings : CounterEffect
}
```

**Key Points:**
- Events are user actions or system inputs
- State contains everything the UI needs to render
- Effects are one-time actions (navigation, toasts, etc.)

## Step 2: Create the ViewModel Interface

```kotlin
// CounterViewModel.kt - Contract for the ViewModel
interface CounterViewModel {
    val state: StateFlow<CounterUiState>
    val effects: Flow<CounterEffect>
    fun input(event: CounterEvent)
}
```

## Step 3: Implement the USF ViewModel

```kotlin
// CounterViewModelImpl.kt
@ContributesBinding(CounterScope::class, boundType = CounterViewModel::class)
@SingleIn(CounterScope::class)
@Inject
class CounterViewModelImpl(
    coroutineScope: CoroutineScope,
    private val repository: CounterRepository, // Optional dependency
) : CounterViewModel,
    UsfViewModel<CounterEvent, CounterUiState, CounterEffect>(
        coroutineScope = coroutineScope,
    ) {

    // Define initial state
    override fun initialState() = CounterUiState(
        onIncrement = { input(CounterEvent.Increment) },
        onDecrement = { input(CounterEvent.Decrement) },
        onReset = { input(CounterEvent.Reset) },
        onAsyncIncrement = { input(CounterEvent.AsyncIncrement) },
    )

    // Process events and update state/emit effects
    override suspend fun ResultScope<CounterUiState, CounterEffect>.process(
        event: CounterEvent
    ) {
        when (event) {
            is CounterEvent.Increment -> {
                updateState { it.copy(count = it.count + 1) }
                emitEffect(CounterEffect.ShowToast("Counter incremented!"))
            }

            is CounterEvent.Decrement -> {
                val currentCount = state.value.count
                if (currentCount > 0) {
                    updateState { it.copy(count = currentCount - 1) }
                } else {
                    emitEffect(CounterEffect.ShowToast("Counter cannot go below 0"))
                }
            }

            is CounterEvent.Reset -> {
                updateState { it.copy(count = 0, errorMessage = null) }
                emitEffect(CounterEffect.ShowToast("Counter reset"))
            }

            is CounterEvent.AsyncIncrement -> {
                updateState { it.copy(isLoading = true, errorMessage = null) }

                try {
                    // Simulate async operation
                    delay(1000)
                    val newValue = repository.incrementAsync(state.value.count)
                    updateState {
                        it.copy(count = newValue, isLoading = false)
                    }
                    emitEffect(CounterEffect.ShowToast("Async increment completed!"))
                } catch (e: Exception) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to increment: ${e.message}"
                        )
                    }
                    emitEffect(CounterEffect.ShowToast("Error: ${e.message}"))
                }
            }
        }
    }

    // Optional: Initialize when UI subscribes
    override fun ResultScope<CounterUiState, CounterEffect>.onSubscribed() {
        logcat { "[CNT] Counter screen subscribed" }
        // Could load initial data here
    }

    // Optional: Cleanup when UI unsubscribes
    override fun onUnsubscribed() {
        logcat { "[CNT] Counter screen unsubscribed" }
        // Cleanup if needed
    }
}
```

**Key Points:**
- `ResultScope` provides thread-safe `updateState()` and `emitEffect()` functions
- State updates are immutable copies
- Long-running operations can be handled with proper loading states
- Error handling should update state and optionally emit effects

## Step 4: Create the Composable Screen

```kotlin
// CounterScreen.kt
@Inject
@SingleIn(CounterScope::class)
class CounterScreen(
    private val viewModel: CounterViewModel,
    private val navigator: Navigator,
) {
    @Composable
    operator fun invoke() {
        val uiState by viewModel.state.collectAsState()

        // Handle one-time effects
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is CounterEffect.ShowToast -> {
                        // Show toast - implement based on your toast system
                        logcat { "[CNT] Toast: ${effect.message}" }
                    }
                    is CounterEffect.NavigateToSettings -> {
                        navigator.goTo(SettingsRoute)
                    }
                }
            }
        }

        // UI rendering
        CounterContent(uiState = uiState)
    }
}

@Composable
private fun CounterContent(uiState: CounterUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Count: ${uiState.count}",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buttons using state callbacks
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = uiState.onDecrement,
                enabled = !uiState.isLoading
            ) {
                Text("-")
            }

            Button(
                onClick = uiState.onIncrement,
                enabled = !uiState.isLoading
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = uiState.onAsyncIncrement,
            enabled = !uiState.isLoading
        ) {
            Text("Async +1")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = uiState.onReset,
            enabled = !uiState.isLoading
        ) {
            Text("Reset")
        }
    }
}

// Preview for testing UI
@Preview
@Composable
private fun CounterContentPreview() {
    CounterContent(
        uiState = CounterUiState(
            count = 42,
            isLoading = false
        )
    )
}
```

**Key Points:**
- Use `LaunchedEffect(viewModel)` to collect effects only once
- Disable UI interactions during loading states
- Use previews to test UI without ViewModels

## Step 5: Set Up Dependency Injection

```kotlin
// CounterScope.kt - Define the feature scope
@Scope
@SingleIn(CounterScope::class)
annotation class CounterScope

// CounterComponent.kt - DI component for the feature
@ContributesSubcomponent(CounterScope::class)
@SingleIn(CounterScope::class)
interface CounterComponent {

    // CRITICAL: Provide CoroutineScope for USF ViewModels
    @Provides
    @SingleIn(CounterScope::class)
    fun provideCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Provide any feature-specific dependencies
    @Provides
    @SingleIn(CounterScope::class)
    fun provideCounterRepository(): CounterRepository =
        CounterRepositoryImpl()

    // Expose the screen for navigation
    val counterScreen: CounterScreen

    @ContributesSubcomponent.Factory(AppScope::class)
    interface Factory {
        fun createCounterComponent(): CounterComponent

        @Provides
        @IntoSet
        fun provideCounterEntryProvider(
            factory: Factory
        ): EntryProviderInstaller = {
            val component by lazy { factory.createCounterComponent() }

            // Register navigation entry
            entry<CounterRoute> { component.counterScreen() }
        }
    }
}
```

**Critical Points:**
- **MUST** provide `CoroutineScope` - USF requires this for pipeline management
- Use `SupervisorJob() + Dispatchers.Main.immediate` for the scope
- Feature components should be self-contained with their dependencies

## Step 6: Add Navigation Route

```kotlin
// CounterRoutes.kt
@Serializable
data object CounterRoute
```

## Step 7: Basic Testing

```kotlin
// CounterViewModelTest.kt
class CounterViewModelTest {

    @Test
    fun `increment increases count and shows toast`() = runTest {
        val viewModel = CounterViewModelImpl(
            coroutineScope = TestScope().backgroundScope,
            repository = FakeCounterRepository()
        )

        val states = mutableListOf<CounterUiState>()
        val effects = mutableListOf<CounterEffect>()

        backgroundScope.launch { viewModel.state.toList(states) }
        backgroundScope.launch { viewModel.effects.toList(effects) }
        runCurrent() // Collect initial state

        // When: increment event
        viewModel.input(CounterEvent.Increment)
        runCurrent()

        // Then: count increased
        assertThat(states.last().count).isEqualTo(1)

        // And: toast effect emitted
        assertThat(effects.last()).isEqualTo(
            CounterEffect.ShowToast("Counter incremented!")
        )
    }

    @Test
    fun `async increment shows loading state`() = runTest {
        val viewModel = CounterViewModelImpl(
            coroutineScope = TestScope().backgroundScope,
            repository = FakeCounterRepository(delay = 100.milliseconds)
        )

        val states = mutableListOf<CounterUiState>()
        backgroundScope.launch { viewModel.state.toList(states) }
        runCurrent()

        // When: async increment
        viewModel.input(CounterEvent.AsyncIncrement)
        runCurrent()

        // Then: loading state is active
        assertThat(states.last().isLoading).isTrue()

        // When: operation completes
        advanceTimeBy(150.milliseconds)
        runCurrent()

        // Then: loading is false and count increased
        assertThat(states.last().isLoading).isFalse()
        assertThat(states.last().count).isEqualTo(1)
    }
}
```

## Step 8: Module Integration

Add to your feature's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":common:usf"))
    implementation(project(":common:log"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.runtime)
    // ... other dependencies
}
```

## Congratulations! 🎉

You've created your first USF feature. Here's what you learned:

### Core USF Concepts
- **Unidirectional Flow**: Events → Process → State/Effects
- **Immutable State**: Always create new state copies
- **Pipeline Management**: USF handles lifecycle automatically
- **Thread Safety**: `ResultScope` ensures safe state updates

### Best Practices Applied
- Clear separation between Events, State, and Effects
- Proper error handling with loading states
- Testing with coroutine test utilities
- Dependency injection with scoped components

## Next Steps

1. **Read Advanced Docs**: Check out `usf-plugin-architecture.md` for complex features
2. **Add More Features**: Try adding persistence, validation, or search
3. **Optimize Performance**: Learn about state update batching and recomposition
4. **Write More Tests**: Cover edge cases and error scenarios

## Common Gotchas

- **Missing CoroutineScope**: USF ViewModels MUST have an injected `CoroutineScope`
- **Blocking `process()` Function**: Long operations should use `coroutineScope.launch`
- **State Mutations**: Never mutate state directly, always use `updateState { }`
- **Effect Collection**: Always use `LaunchedEffect(viewModel)` to collect effects once

For troubleshooting common issues, see [USF Troubleshooting](#usf-troubleshooting).

# USF Testing Guide

> **Comprehensive guide** for testing USF ViewModels and Plugins with practical examples and best practices.

## Testing Philosophy

USF makes testing predictable through:
- **Pure Functions**: `process()` functions are deterministic
- **Clear Boundaries**: Events in, State/Effects out
- **No Hidden State**: Everything flows through the pipeline
- **Isolated Logic**: ViewModels can be tested without UI

## Quick Setup

### Test Dependencies
```kotlin
// build.gradle.kts
testImplementation(libs.junit.jupiter)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.mockk)
testImplementation(libs.assertj.core)
testImplementation(libs.turbine) // For Flow testing
```

### Basic Test Structure
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    @JvmField @RegisterExtension
    val coroutineTestRule = CoroutineTestRule()

    private fun TestScope.createViewModel(): MyViewModelImpl {
        return MyViewModelImpl(
            coroutineScope = backgroundScope,
            repository = mockRepository,
            // Use test dispatcher for predictable timing
            processingDispatcher = coroutineTestRule.testDispatcher
        )
    }

    @Test
    fun `test description in backticks`() = runTest {
        // Arrange
        val viewModel = createViewModel()

        // Act & Assert
        // Your test logic
    }
}
```

## Testing Patterns

### 1. State Testing

#### Simple State Updates
```kotlin
@Test
fun `increment increases count by one`() = runTest {
    val viewModel = createViewModel()

    val states = mutableListOf<CounterUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent() // Collect initial state

    // Act
    viewModel.input(CounterEvent.Increment)
    runCurrent()

    // Assert
    assertThat(states).hasSize(2) // Initial + Updated
    assertThat(states.last().count).isEqualTo(1)
}
```

#### Complex State Changes
```kotlin
@Test
fun `loading data updates state through all phases`() = runTest {
    // Mock repository behavior
    coEvery { mockRepository.fetchData() } coAnswers {
        delay(100) // Simulate network delay
        listOf(Item("test"))
    }

    val viewModel = createViewModel()
    val states = mutableListOf<DataUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    // Trigger load
    viewModel.input(DataEvent.LoadData)
    runCurrent() // Process event immediately

    // Assert loading state
    assertThat(states.last()).isEqualTo(
        DataUiState(
            loading = true,
            data = emptyList(),
            error = null
        )
    )

    // Complete async operation
    advanceTimeBy(150) // Past the delay
    runCurrent()

    // Assert final state
    assertThat(states.last()).isEqualTo(
        DataUiState(
            loading = false,
            data = listOf(Item("test")),
            error = null
        )
    )
}
```

#### Using Turbine for Flow Testing
```kotlin
@Test
fun `state updates flow correctly with turbine`() = runTest {
    val viewModel = createViewModel()

    viewModel.state.test {
        // Initial state
        assertThat(awaitItem()).isEqualTo(CounterUiState(count = 0))

        // Trigger event
        viewModel.input(CounterEvent.Increment)

        // Updated state
        assertThat(awaitItem()).isEqualTo(CounterUiState(count = 1))

        cancelAndIgnoreRemainingEvents()
    }
}
```

### 2. Effect Testing

#### Single Effect Emission
```kotlin
@Test
fun `decrement below zero shows error toast`() = runTest {
    val viewModel = createViewModel() // Initial count = 0

    val effects = mutableListOf<CounterEffect>()
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act
    viewModel.input(CounterEvent.Decrement)
    runCurrent()

    // Assert
    assertThat(effects).containsExactly(
        CounterEffect.ShowToast("Counter cannot go below 0")
    )
}
```

#### Multiple Effects
```kotlin
@Test
fun `successful operation emits multiple effects`() = runTest {
    val viewModel = createViewModel()

    viewModel.effects.test {
        // Act
        viewModel.input(ProcessEvent.StartComplexOperation)

        // Assert effects in order
        assertThat(awaitItem()).isEqualTo(Effect.ShowProgress)
        assertThat(awaitItem()).isEqualTo(Effect.ShowToast("Processing..."))
        assertThat(awaitItem()).isEqualTo(Effect.NavigateToResult)

        cancelAndIgnoreRemainingEvents()
    }
}
```

### 3. Error Handling Testing

#### Exception Handling
```kotlin
@Test
fun `repository error updates state and emits effect`() = runTest {
    // Arrange
    val exception = IOException("Network error")
    coEvery { mockRepository.fetchData() } throws exception

    val viewModel = createViewModel()
    val states = mutableListOf<UiState>()
    val effects = mutableListOf<Effect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act
    viewModel.input(Event.LoadData)
    runCurrent()

    // Assert state
    assertThat(states.last()).isEqualTo(
        UiState(
            loading = false,
            hasError = true,
            errorMessage = "Network error. Check your connection."
        )
    )

    // Assert effect
    assertThat(effects.last()).isEqualTo(
        Effect.ShowErrorToast("Network error")
    )
}
```

#### Retry Logic Testing
```kotlin
@Test
fun `retry logic attempts operation multiple times`() = runTest {
    // Arrange - fail first 2 attempts, succeed on 3rd
    coEvery { mockRepository.performOperation() } throws IOException("Temporary error") andThenThrows IOException("Still failing") andThen "success"

    val viewModel = createViewModel()

    // Act
    viewModel.input(Event.PerformWithRetry)
    advanceUntilIdle() // Let all retries complete

    // Assert
    coVerify(exactly = 3) { mockRepository.performOperation() }

    // Verify final state is success
    assertThat(viewModel.state.value.result).isEqualTo("success")
}
```

### 4. Lifecycle Testing

#### Subscription Lifecycle
```kotlin
@Test
fun `onSubscribed called when first subscriber connects`() = runTest {
    val viewModel = createViewModel()

    // No subscribers yet
    coVerify(exactly = 0) { mockRepository.initialize() }

    // Start collecting
    val job = backgroundScope.launch {
        viewModel.state.collect { /* consume */ }
    }
    runCurrent()

    // Verify initialization called
    coVerify(exactly = 1) { mockRepository.initialize() }

    job.cancel()
}
```

#### Unsubscription Timeout
```kotlin
@Test
fun `onUnsubscribed called after timeout when no subscribers`() = runTest {
    val viewModel = createViewModel()

    // Start and stop observing
    val job = backgroundScope.launch {
        viewModel.state.collect { /* consume */ }
    }
    runCurrent()
    job.cancel()

    // Cleanup not called immediately
    coVerify(exactly = 0) { mockRepository.cleanup() }

    // Advance past timeout (5 seconds)
    advanceTimeBy(6000)
    runCurrent()

    // Now cleanup is called
    coVerify(exactly = 1) { mockRepository.cleanup() }
}
```

#### Rapid Resubscription
```kotlin
@Test
fun `rapid resubscription prevents cleanup`() = runTest {
    val viewModel = createViewModel()

    // Subscribe
    val job1 = backgroundScope.launch { viewModel.state.collect { } }
    runCurrent()

    // Unsubscribe and quickly resubscribe
    job1.cancel()
    advanceTimeBy(2000) // Only 2 seconds

    val job2 = backgroundScope.launch { viewModel.state.collect { } }
    runCurrent()

    // Advance past original timeout
    advanceTimeBy(4000) // Total 6 seconds
    runCurrent()

    // Cleanup should not have been called
    coVerify(exactly = 0) { mockRepository.cleanup() }

    job2.cancel()
}
```

### 5. Async Operation Testing

#### Coroutine Cancellation
```kotlin
@Test
fun `cancellation properly handles ongoing operations`() = runTest {
    // Mock with delay
    coEvery { mockRepository.longRunningOperation() } coAnswers {
        delay(5000)
        "result"
    }

    val viewModel = createViewModel()
    val states = mutableListOf<UiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    // Start operation
    viewModel.input(Event.StartLongOperation)
    runCurrent()

    // Verify loading state
    assertThat(states.last().loading).isTrue()

    // Cancel operation
    viewModel.input(Event.CancelOperation)
    runCurrent()

    // Verify loading stopped
    assertThat(states.last().loading).isFalse()

    // Advance time - operation should not complete
    advanceTimeBy(6000)
    assertThat(states.last().result).isNull()
}
```

#### Race Condition Testing
```kotlin
@Test
fun `concurrent operations handle race conditions correctly`() = runTest {
    coEvery { mockRepository.search(any()) } coAnswers {
        delay(arg<String>(0).length * 100L) // Simulate variable delay
        "result for ${arg<String>(0)}"
    }

    val viewModel = createViewModel()
    val states = mutableListOf<SearchUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    // Start multiple searches rapidly
    viewModel.input(SearchEvent.Query("a"))      // 100ms delay
    viewModel.input(SearchEvent.Query("ab"))     // 200ms delay
    viewModel.input(SearchEvent.Query("abc"))    // 300ms delay

    // Let all complete
    advanceUntilIdle()

    // Only the last search result should be in state
    assertThat(states.last().results).isEqualTo("result for abc")
}
```

### 6. Complex Scenario Testing

#### Multi-Step Workflow
```kotlin
@Test
fun `complete workflow from start to finish`() = runTest {
    val viewModel = createViewModel()
    val states = mutableListOf<WorkflowUiState>()
    val effects = mutableListOf<WorkflowEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Step 1: Initialize
    viewModel.input(WorkflowEvent.Initialize)
    runCurrent()
    assertThat(states.last().step).isEqualTo(WorkflowStep.INITIALIZED)

    // Step 2: Process
    coEvery { mockService.process(any()) } returns ProcessResult.Success("data")
    viewModel.input(WorkflowEvent.Process("input"))
    runCurrent()

    assertThat(states.last().step).isEqualTo(WorkflowStep.PROCESSING)

    // Let async complete
    advanceUntilIdle()

    // Step 3: Complete
    assertThat(states.last().step).isEqualTo(WorkflowStep.COMPLETED)
    assertThat(effects.last()).isEqualTo(WorkflowEffect.NavigateToSuccess)
}
```

## Plugin Testing

### Testing Plugins in Isolation

```kotlin
class SearchPluginTest {

    @Test
    fun `search plugin handles query changes with debounce`() = runTest {
        val plugin = SearchPlugin<Product> { query ->
            productRepository.search(query)
        }

        val mockScope = mockk<ResultScope<SearchState<Product>, SearchEffect>>()
        val states = mutableListOf<SearchState<Product>>()

        every { mockScope.updateState(any()) } answers {
            val updater = arg<(SearchState<Product>) -> SearchState<Product>>(0)
            states.add(updater(states.lastOrNull() ?: SearchState()))
        }

        // Rapid queries
        plugin.run { mockScope.process(SearchEvent.QueryChanged("a")) }
        plugin.run { mockScope.process(SearchEvent.QueryChanged("ab")) }
        plugin.run { mockScope.process(SearchEvent.QueryChanged("abc")) }

        // Advance past debounce
        advanceTimeBy(400)
        runCurrent()

        // Only final query should have triggered search
        assertThat(states.last().query).isEqualTo("abc")
        coVerify(exactly = 1) { productRepository.search("abc") }
    }
}
```

### Testing ViewModel with Plugins

```kotlin
@Test
fun `view model with search plugin integrates correctly`() = runTest {
    val searchPlugin = mockk<SearchPlugin<Product>>()
    val viewModel = ProductViewModelImpl(
        coroutineScope = backgroundScope,
        searchPlugin = searchPlugin
    )

    coEvery {
        searchPlugin.process(any<SearchEvent>())
    } coAnswers {
        // Mock plugin updating its internal state
        SearchState(query = "test", results = listOf(Product("test")))
    }

    val states = mutableListOf<ProductUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    // Act
    viewModel.input(ProductEvent.SearchQueryChanged("test"))
    runCurrent()

    // Assert plugin was called
    coVerify { searchPlugin.process(SearchEvent.QueryChanged("test")) }

    // Assert ViewModel state includes plugin data
    assertThat(states.last().searchResults).hasSize(1)
    assertThat(states.last().searchResults.first().name).isEqualTo("test")
}
```

## Test Utilities

### Custom Assertions
```kotlin
// Custom assertion extensions
fun Assert<UiState>.isLoading() = given { actual ->
    if (!actual.loading) {
        failWithActual(simpleFact("expected to be loading"))
    }
}

fun Assert<UiState>.hasError(expectedMessage: String) = given { actual ->
    if (actual.errorMessage != expectedMessage) {
        failWithMessage("Expected error '$expectedMessage' but was '${actual.errorMessage}'")
    }
}

// Usage
assertThat(viewModel.state.value).isLoading()
assertThat(viewModel.state.value).hasError("Network error")
```

### Test Factories
```kotlin
object TestViewModelFactory {
    fun createCounterViewModel(
        testScope: TestScope,
        initialCount: Int = 0,
        repository: CounterRepository = mockk()
    ): CounterViewModelImpl {
        return CounterViewModelImpl(
            coroutineScope = testScope.backgroundScope,
            repository = repository,
            processingDispatcher = testScope.testScheduler
        ).apply {
            // Set initial state if needed
            if (initialCount != 0) {
                repeat(initialCount) { input(CounterEvent.Increment) }
            }
        }
    }
}
```

### Mock Extensions
```kotlin
// Extension to simplify repository mocking
fun MockKStubScope<Repository, suspend () -> Result<Data>>.returnsAfterDelay(
    result: Data,
    delayMs: Long = 100
) = coAnswers {
    delay(delayMs)
    Result.success(result)
}

// Usage
coEvery { mockRepository.fetchData() } returnsAfterDelay(testData, 200)
```

## Performance Testing

### Memory Leak Testing
```kotlin
@Test
fun `view model properly releases resources`() = runTest {
    val viewModel = createViewModel()

    // Create weak reference to track GC
    val weakRef = WeakReference(viewModel)

    // Use ViewModel
    val job = backgroundScope.launch { viewModel.state.collect { } }
    runCurrent()
    job.cancel()

    // Force cleanup
    advanceTimeBy(6000) // Past timeout
    runCurrent()

    // Force garbage collection
    System.gc()
    System.runFinalization()
    System.gc()

    // ViewModel should be collectible
    assertThat(weakRef.get()).isNull()
}
```

### State Update Performance
```kotlin
@Test
fun `frequent state updates perform efficiently`() = runTest {
    val viewModel = createViewModel()
    val states = mutableListOf<CounterUiState>()
    backgroundScope.launch { viewModel.state.toList(states) }
    runCurrent()

    val startTime = System.currentTimeMillis()

    // Perform many rapid updates
    repeat(1000) {
        viewModel.input(CounterEvent.Increment)
    }
    runCurrent()

    val duration = System.currentTimeMillis() - startTime

    // Assert reasonable performance (adjust threshold as needed)
    assertThat(duration).isLessThan(1000) // Less than 1 second
    assertThat(states.last().count).isEqualTo(1000)
}
```

## Best Practices

### DO
- ✅ Test state transitions explicitly
- ✅ Verify effects are emitted correctly
- ✅ Test error conditions and edge cases
- ✅ Use `TestScope` and `runTest` for coroutines
- ✅ Mock dependencies with specific behaviors
- ✅ Test lifecycle hooks (onSubscribed/onUnsubscribed)
- ✅ Use descriptive test names in backticks
- ✅ Separate arrange, act, and assert clearly

### DON'T
- ❌ Test internal implementation details
- ❌ Forget to call `runCurrent()` after triggering events
- ❌ Mix UI testing with ViewModel testing
- ❌ Use `GlobalScope` or real dispatchers in tests
- ❌ Create overly complex test scenarios
- ❌ Ignore cleanup in test lifecycle
- ❌ Test multiple concerns in one test

## Integration with UI Testing

While this guide focuses on ViewModel testing, USF ViewModels integrate well with UI tests:

```kotlin
// In UI tests, create ViewModel with test implementations
@Composable
fun TestableScreen(
    viewModel: MyViewModel = MyViewModelImpl(
        coroutineScope = rememberCoroutineScope(),
        repository = FakeRepository() // Use fake for UI tests
    )
) {
    MyScreen(viewModel = viewModel)
}
```

## Debugging Tests

### Logging State Changes
```kotlin
@Test
fun `debug state changes`() = runTest {
    val viewModel = createViewModel()

    backgroundScope.launch {
        viewModel.state.collectIndexed { index, state ->
            println("State $index: $state")
        }
    }

    // Your test logic...
}
```

### Effect Debugging
```kotlin
@Test
fun `debug effects emission`() = runTest {
    val viewModel = createViewModel()

    backgroundScope.launch {
        viewModel.effects.collect { effect ->
            println("Effect emitted: $effect")
        }
    }

    // Your test logic...
}
```

Testing USF ViewModels is straightforward and reliable. The key is understanding the unidirectional flow and testing each part of the pipeline: Events → State/Effects.

# USF Troubleshooting Guide

> **Common issues** and their solutions when working with USF ViewModels and Plugins.

## Quick Reference

### Most Common Issues
1. [Missing CoroutineScope injection](#missing-coroutinescope-injection)
2. [State not updating](#state-not-updating-in-ui)
3. [Effects not collected](#effects-not-being-collected)
4. [Tests hanging or flaky](#tests-hanging-or-failing)
5. [Memory leaks](#memory-leaks)

## Compilation Issues

### Missing CoroutineScope Injection

**Error:**
```
No suitable constructor found for injection of CoroutineScope
```

**Cause:** USF ViewModels require an injected `CoroutineScope`, but DI component doesn't provide one.

**Solution:**
```kotlin
// Add to your component
@ContributesSubcomponent(YourScope::class)
interface YourComponent {

    @Provides
    @SingleIn(YourScope::class)
    fun provideCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ... rest of component
}
```

### Wrong CoroutineScope Type

**Error:**
```
Type mismatch: inferred type is CoroutineScope but CoroutineScope was expected
```

**Cause:** Using `viewModelScope` instead of injected scope.

**Solution:**
```kotlin
// ❌ Wrong - don't use viewModelScope
class MyViewModel : ViewModel() {
    private val usfViewModel = UsfViewModel<Event, UiState, Effect>(viewModelScope)
}

// ✅ Correct - inject CoroutineScope
@Inject
class MyViewModelImpl(
    coroutineScope: CoroutineScope  // Injected from DI
) : UsfViewModel<Event, UiState, Effect>(coroutineScope)
```

### ResultScope Not Found

**Error:**
```
Unresolved reference: ResultScope
```

**Cause:** Missing import or wrong scope context.

**Solution:**
```kotlin
import sh.kau.playground.usf.scope.ResultScope

// Ensure process function uses correct signature
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    // Your implementation
}
```

## Runtime Issues

### State Not Updating in UI

**Symptoms:**
- UI doesn't reflect state changes
- State seems stuck at initial values
- No recomposition happening

**Common Causes & Solutions:**

#### 1. Not Calling `runCurrent()` in Tests
```kotlin
// ❌ Wrong
@Test
fun `test state update`() = runTest {
    viewModel.input(Event.Update)
    // Missing runCurrent() - state won't update
    assertThat(viewModel.state.value.updated).isTrue()
}

// ✅ Correct
@Test
fun `test state update`() = runTest {
    viewModel.input(Event.Update)
    runCurrent() // Process the event
    assertThat(viewModel.state.value.updated).isTrue()
}
```

#### 2. State Updates in Wrong Context
```kotlin
// ❌ Wrong - updateState outside ResultScope
override suspend fun process(event: Event) {
    coroutineScope.launch {
        updateState { it.copy(loading = false) } // Won't work!
    }
}

// ✅ Correct - updateState in ResultScope
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    coroutineScope.launch {
        val data = repository.fetch()
        updateState { it.copy(data = data) } // Works!
    }
}
```

#### 3. Mutating State Instead of Copying
```kotlin
// ❌ Wrong - mutating existing state
data class MyState(val items: MutableList<Item> = mutableListOf())

updateState { state ->
    state.items.add(newItem) // Mutates existing state!
    state // Returns same reference - no recomposition
}

// ✅ Correct - creating new state
data class MyState(val items: List<Item> = emptyList())

updateState { state ->
    state.copy(items = state.items + newItem) // New state instance
}
```

### Effects Not Being Collected

**Symptoms:**
- Navigation not working
- Toasts not showing
- Side effects not happening

**Common Causes & Solutions:**

#### 1. Missing LaunchedEffect in UI
```kotlin
// ❌ Wrong - effects not collected
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.state.collectAsState()
    // Missing effect collection!

    MyContent(uiState)
}

// ✅ Correct - collecting effects
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.Navigate -> navigator.goTo(effect.route)
                is Effect.ShowToast -> showToast(effect.message)
            }
        }
    }

    MyContent(uiState)
}
```

#### 2. Wrong LaunchedEffect Key
```kotlin
// ❌ Wrong - effect collection restarts on every state change
LaunchedEffect(uiState) {
    viewModel.effects.collect { /* handle */ }
}

// ✅ Correct - effect collection stable across state changes
LaunchedEffect(viewModel) {
    viewModel.effects.collect { /* handle */ }
}
```

### Tests Hanging or Failing

#### 1. Not Using TestScope
```kotlin
// ❌ Wrong - real coroutine scope in tests
val viewModel = MyViewModelImpl(
    coroutineScope = CoroutineScope(Dispatchers.IO)
)

// ✅ Correct - TestScope for controlled execution
val viewModel = MyViewModelImpl(
    coroutineScope = TestScope().backgroundScope
)
```

#### 2. Not Advancing Time for Delays
```kotlin
// ❌ Wrong - real delays in tests
@Test
fun `search with debounce`() = runTest {
    viewModel.input(SearchEvent.Query("test"))
    delay(500) // Real delay - test becomes slow
    assertThat(viewModel.state.value.results).isNotEmpty()
}

// ✅ Correct - advance test time
@Test
fun `search with debounce`() = runTest {
    viewModel.input(SearchEvent.Query("test"))
    advanceTimeBy(500) // Advance virtual time
    runCurrent()
    assertThat(viewModel.state.value.results).isNotEmpty()
}
```

#### 3. Missing Mock Interactions
```kotlin
// ❌ Wrong - mock doesn't return anything
coEvery { mockRepository.fetch() } returns Unit // Wrong return type

// ✅ Correct - proper mock setup
coEvery { mockRepository.fetch() } returns listOf(item1, item2)
```

## Performance Issues

### Memory Leaks

**Symptoms:**
- App memory usage grows over time
- ViewModels not being garbage collected
- Background operations continue after navigation

**Common Causes & Solutions:**

#### 1. Long-Running Operations Not Cancelled
```kotlin
// ❌ Wrong - job continues after unsubscribe
private var longRunningJob: Job? = null

override fun onSubscribed() {
    longRunningJob = coroutineScope.launch {
        while (true) {
            delay(1000)
            // This will continue running!
        }
    }
}

// ✅ Correct - cancel job on unsubscribe
override fun onUnsubscribed() {
    longRunningJob?.cancel()
    longRunningJob = null
}
```

#### 2. External References Holding ViewModel
```kotlin
// ❌ Wrong - static reference prevents GC
object GlobalCache {
    var viewModel: MyViewModel? = null // Holds reference!
}

// ✅ Correct - use WeakReference or clear explicitly
object GlobalCache {
    private var weakViewModel: WeakReference<MyViewModel>? = null

    fun setViewModel(vm: MyViewModel) {
        weakViewModel = WeakReference(vm)
    }
}
```

### Excessive Recomposition

**Symptoms:**
- UI feels laggy
- Excessive logging in Composables
- Battery drain

**Common Causes & Solutions:**

#### 1. State Updates Too Frequent
```kotlin
// ❌ Wrong - updating state on every character
override suspend fun process(event: SearchEvent.TextChanged) {
    repository.searchItems(event.text) // Network call on every character!
        .collect { results ->
            updateState { it.copy(results = results) }
        }
}

// ✅ Correct - debounce updates
private var searchJob: Job? = null

override suspend fun process(event: SearchEvent.TextChanged) {
    searchJob?.cancel()
    updateState { it.copy(query = event.text) } // Update query immediately

    searchJob = coroutineScope.launch {
        delay(300) // Debounce
        repository.searchItems(event.text)
            .collect { results ->
                updateState { it.copy(results = results) }
            }
    }
}
```

#### 2. Unstable Lambdas in State
```kotlin
// ❌ Wrong - lambda recreated on each state update
data class UiState(
    val onItemClick: (Item) -> Unit = { item ->
        // This lambda is recreated every time!
        processItem(item)
    }
)

// ✅ Correct - stable callbacks or direct input
data class UiState(
    val onItemClick: (Item) -> Unit = {}
)

override fun initialState() = UiState(
    onItemClick = { item -> input(Event.ItemClicked(item)) }
)
```

## Debugging Tips

### Enable Debug Logging

Add custom inspector to see USF pipeline activity:

```kotlin
class DebugUsfInspector : UsfInspector {
    override fun onEventReceived(event: Any) {
        logcat { "[USF] Event: $event" }
    }

    override fun onStateUpdated(oldState: Any, newState: Any) {
        logcat { "[USF] State: $oldState -> $newState" }
    }

    override fun onEffectEmitted(effect: Any) {
        logcat { "[USF] Effect: $effect" }
    }
}

// Use in ViewModel
class MyViewModelImpl(
    coroutineScope: CoroutineScope
) : UsfViewModel<Event, UiState, Effect>(
    coroutineScope = coroutineScope,
    inspector = if (BuildConfig.DEBUG) DebugUsfInspector() else null
)
```

### State Flow Debugging

Track state changes over time:

```kotlin
@Test
fun `debug state flow`() = runTest {
    val states = mutableListOf<MyState>()
    val job = backgroundScope.launch {
        viewModel.state.collectIndexed { index, state ->
            println("State $index: $state")
            states.add(state)
        }
    }

    // Your test logic...

    println("Final states: ${states.size}")
    job.cancel()
}
```

### Effect Flow Debugging

Track effect emissions:

```kotlin
@Test
fun `debug effects`() = runTest {
    val effects = mutableListOf<MyEffect>()
    val job = backgroundScope.launch {
        viewModel.effects.collect { effect ->
            println("Effect: $effect")
            effects.add(effect)
        }
    }

    // Your test logic...

    println("Effects emitted: $effects")
    job.cancel()
}
```

## Plugin-Specific Issues

### Plugin Events Not Processing

**Cause:** Event not mapped to plugin correctly.

**Solution:**
```kotlin
// Check your event mapping
register(
    plugin = myPlugin,
    mapEvent = { vmEvent ->
        when (vmEvent) {
            is VmEvent.PluginEvent -> PluginEvent.Process(vmEvent.data)
            else -> null // Make sure this covers all plugin events
        }
    }
)
```

### Plugin State Not Applying

**Cause:** State adapter not merging plugin state correctly.

**Solution:**
```kotlin
// Check your state adapter
applyState = { vmState, pluginState ->
    vmState.copy(
        // Make sure all relevant plugin state is copied
        pluginData = pluginState.data,
        pluginLoading = pluginState.loading,
        // Don't forget any fields!
    )
}
```

## Getting Help

### Check These First
1. ✅ All required imports present
2. ✅ CoroutineScope properly injected
3. ✅ ResultScope used in process function
4. ✅ Effects collected in UI
5. ✅ Tests use TestScope and runCurrent()

### Enable More Logging
```kotlin
// Add to your logging configuration
if (BuildConfig.DEBUG) {
    logcat.LogPriority.VERBOSE // Enable verbose logging
}
```

### Minimal Reproduction
Create a simple test case that reproduces your issue:

```kotlin
@Test
fun `minimal reproduction of issue`() = runTest {
    val viewModel = createMinimalViewModel()

    // Simplest possible steps to reproduce
    viewModel.input(MinimalEvent.Trigger)
    runCurrent()

    // What you expected vs what happened
    assertThat(viewModel.state.value.shouldWork).isTrue()
}
```

Remember: USF is predictable - Events flow to State/Effects through the pipeline. When something's not working, trace the flow step by step.
