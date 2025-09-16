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
) : UsfViewModel<Event, State, Effect>(coroutineScope) {
    override fun initialState() = State()

    override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
override fun ResultScope<State, Effect>.onSubscribed() {
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
│   Cleanup   │◀──────────────────│   Timeout   │
│             │                    │             │
│ • Flows     │                    │ • Paused    │
│   closed    │                    │ • 5s timer  │
│ • Resources │                    │ • Can       │
│   released  │                    │   resume    │
└─────────────┘                    └─────────────┘
```

### Lifecycle Hooks

```kotlin
override fun ResultScope<State, Effect>.onSubscribed() {
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

override fun ResultScope<State, Effect>.onSubscribed() {
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

override fun ResultScope<State, Effect>.onSubscribed() {
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
) : UsfViewModel<Event, State, Effect>(
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
class ComplexViewModel : UsfViewModel<Event, State, Effect>() {
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

See `.ai/docs/usf-plugin-architecture.md` for complete implementation details.

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
private suspend fun ResultScope<State, Effect>.performWithRetry(
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

override suspend fun ResultScope<State, Effect>.process(event: Event) {
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

override suspend fun ResultScope<State, Effect>.process(event: Event) {
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

override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
) : UsfViewModel<Event, State, Effect>(coroutineScope) {

    override fun initialState() = State()

    override suspend fun ResultScope<State, Effect>.process(event: Event) {
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