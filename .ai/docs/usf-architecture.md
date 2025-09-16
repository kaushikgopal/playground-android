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

USF ViewModels have smart resource management:

1. **Dormant** - No UI observing, no resources
2. **Active** - UI subscribed, processing events
3. **Timeout** - UI gone, 5-second countdown
4. **Cleanup** - Resources released if UI doesn't return

This happens automatically - ViewModels:
- Start when UI appears
- Pause when UI is gone
- Resume if UI returns quickly
- Clean up if UI is gone for good

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

USF makes complex ViewModels manageable and simple ViewModels trivial.