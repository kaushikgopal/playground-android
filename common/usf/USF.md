# USF (Unidirectional State Flow) - Core Patterns

> **For project overview**, see root @AGENTS.md
> **For complete tutorial**, see @USF-QUICKSTART.md
> **For advanced plugins**, see @USF-PLUGINS.md
> **For comprehensive testing**, see @USF-TESTING.md
> **For troubleshooting**, see @USF-TROUBLESHOOTING.md

This guide covers the core USF patterns you'll use in 90% of cases.

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

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI Layer                               │
│  ┌─────────────────┐    ┌──────────────────────────────────┐    │
│  │   MyScreen.kt   │────│        Compose UI Elements       │    │
│  │ @Composable     │    │   Button(onClick = uiState.      │    │
│  │ operator invoke │    │           onButtonClicked)       │    │
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

### The Three Types

```kotlin
// 1. Events - User actions
sealed interface HomeEvent {
    data object RefreshClicked : HomeEvent
    data class ItemClicked(val itemId: String) : HomeEvent
}

// 2. State - UI display (with callbacks)
data class HomeUiState(
    val title: String = "Home",
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    // Callbacks as part of state
    val onRefreshClicked: () -> Unit = {},
    val onItemClicked: (String) -> Unit = {},
)

// 3. Effects - One-time actions
sealed interface HomeEffect {
    data object NavigateToSettings : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}
```

### Naming Convention

For screens with longer names, use abbreviated prefixes:

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

## Basic ViewModel Implementation

### Constructor Pattern

The `UsfViewModel` constructor accepts these parameters:

```kotlin
abstract class UsfViewModel<Event : Any, UiState : Any, Effect : Any>(
    coroutineScope: CoroutineScope,                          // Required: Injected scope
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,  // Optional
    inspector: UsfInspector? = null,                         // Optional: debugging
)
```

**Key Points:**
- **coroutineScope**: inject via DI - don't use `viewModelScope` directly
- **processingDispatcher**: Defaults to `Dispatchers.IO`, override for testing
- **inspector**: Optional monitoring for debugging/analytics

### Complete ViewModel Example

```kotlin
@ContributesBinding(FeatureScope::class, boundType = MyViewModel::class)
@SingleIn(FeatureScope::class)
@Inject
class MyViewModelImpl(
    coroutineScope: CoroutineScope,
    private val repository: MyRepository,
) : MyViewModel,
    UsfViewModel<MyEvent, MyUiState, MyEffect>(
        coroutineScope = coroutineScope,
    ) {

    // Single expression for simple initial states
    override fun initialState() = MyUiState(
        title = "Title",
        onButtonClicked = { input(MyEvent.ButtonClicked) },
        onTextChanged = { text -> input(MyEvent.TextChanged(text)) },
    )

    override suspend fun ResultScope<MyUiState, MyEffect>.process(event: MyEvent) {
        when (event) {
            is MyEvent.ButtonClicked -> {
                logcat { "[TAG] Button clicked" }
                updateState { it.copy(loading = true) }
                val result = repository.doWork()
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
    }

    override fun onUnsubscribed() {
        logcat { "[TAG] Unsubscribed" }
        // Cleanup resources
    }
}
```

### The ResultScope

`ResultScope` provides thread-safe operations inside `process()`:

```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    // Update state (thread-safe, immutable)
    updateState { currentState ->
        currentState.copy(loading = true)
    }

    // Emit effects (one-time actions)
    emitEffect(Effect.ShowToast("Hello"))

    // Access current state
    val currentValue = state.value.someField

    // Launch coroutines (automatically scoped)
    coroutineScope.launch {
        // Async work here
    }
}
```

### Common Patterns

#### Loading States
```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
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

#### Error Handling
```kotlin
data class UiState(
    val data: List<Item> = emptyList(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadData -> {
            updateState { it.copy(loading = true, errorMessage = null) }
            try {
                val data = repository.fetchData()
                updateState { it.copy(loading = false, data = data) }
            } catch (e: Exception) {
                logcat { "[TAG] Error: ${e.message}" }
                updateState {
                    it.copy(
                        loading = false,
                        errorMessage = when (e) {
                            is NetworkException -> "Network error. Check connection."
                            is AuthException -> "Auth failed. Please log in."
                            else -> "Something went wrong. Try again."
                        }
                    )
                }
                emitEffect(Effect.ShowErrorToast(e.message))
            }
        }
    }
}
```

#### Debounced Operations
```kotlin
private var searchJob: Job? = null

override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.SearchTextChanged -> {
            searchJob?.cancel()
            updateState { it.copy(query = event.text) }

            searchJob = coroutineScope.launch {
                delay(300)  // Debounce
                val results = repository.search(event.text)
                updateState { it.copy(results = results) }
            }
        }
    }
}
```

#### Form Validation
```kotlin
data class FormState(
    val email: String = "",
    val isValid: Boolean = false,
)

override suspend fun ResultScope<FormState, Effect>.process(event: Event) {
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

### Lifecycle Hooks

```kotlin
// Called when first UI subscriber connects
override fun ResultScope<UiState, Effect>.onSubscribed() {
    logcat { "[TAG] UI subscribed - pipeline active" }

    // Perfect for:
    // - Loading initial data
    // - Starting background tasks
    // - Establishing connections

    coroutineScope.launch {
        val data = repository.loadCachedData()
        updateState { it.copy(data = data) }
    }
}

// Called when last subscriber disconnects (5-second timeout)
override fun onUnsubscribed() {
    logcat { "[TAG] UI unsubscribed - cleanup after 5s timeout" }

    // Perfect for:
    // - Cleanup expensive resources
    // - Close connections
    // - Cancel background work

    repository.closeConnections()
}
```

### Callbacks in State

⚠️ **Important:** Prefer direct `viewModel.input()` calls when possible for better testability.

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

## UI Integration

### Screen Implementation

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
                        navigator.goTo(NextRoute)
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

    // CRITICAL: Provide CoroutineScope for USF ViewModels
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

**Critical Points:**
- **MUST** provide `CoroutineScope` - USF requires this for pipeline management
- Use `SupervisorJob() + Dispatchers.Main.immediate` for the scope
- Feature components should be self-contained with their dependencies

### File Structure

Each USF feature follows consistent organization:

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

## Testing Essentials

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

### Testing State Updates

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

### Testing Effects

```kotlin
@Test
fun `button click shows toast`() = runTest {
    val viewModel = createViewModel()

    val effects = mutableListOf<MyEffect>()
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act
    viewModel.input(MyEvent.ButtonClicked)
    runCurrent()

    // Assert
    assertThat(effects).containsExactly(
        MyEffect.ShowToast("Clicked!")
    )
}
```

### Testing Async Operations

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
    assertThat(states.last().loading).isTrue()

    // Complete async operation
    advanceTimeBy(150) // Past the delay
    runCurrent()

    // Assert final state
    assertThat(states.last().loading).isFalse()
    assertThat(states.last().data).hasSize(1)
}
```

### Testing Error Handling

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
    assertThat(states.last().errorMessage).contains("Network error")

    // Assert effect
    assertThat(effects.last()).isInstanceOf(Effect.ShowErrorToast::class.java)
}
```

**See @USF-TESTING.md for comprehensive testing patterns, utilities, and debugging strategies.**

## Quick Troubleshooting

### Missing CoroutineScope

**Error:** `No suitable constructor found for injection of CoroutineScope`

**Solution:** Add to your DI component:
```kotlin
@Provides
@SingleIn(YourScope::class)
fun provideCoroutineScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
```

### State Not Updating

**Problem:** UI doesn't reflect state changes

**Common causes:**
1. Not calling `runCurrent()` in tests after `input()`
2. Mutating state instead of creating new copy
3. `updateState` called outside `ResultScope`

**Solution:** Always use immutable updates:
```kotlin
// ✅ Correct
updateState { it.copy(newValue = value) }

// ❌ Wrong - mutates existing state
updateState { state ->
    state.items.add(newItem)
    state
}
```

### Effects Not Collected

**Problem:** Navigation/toasts not working

**Solution:** Add `LaunchedEffect` in composable:
```kotlin
LaunchedEffect(viewModel) {  // Key on viewModel, not state!
    viewModel.effects.collect { effect ->
        when (effect) {
            is Effect.Navigate -> navigator.goTo(effect.route)
        }
    }
}
```

### Tests Hanging

**Problem:** Tests never complete

**Common causes:**
1. Using real `CoroutineScope` instead of `TestScope`
2. Not advancing time with `advanceTimeBy()` for delays
3. Missing `runCurrent()` after triggering events

**Solution:**
```kotlin
// ✅ Correct
val viewModel = MyViewModelImpl(
    coroutineScope = TestScope().backgroundScope
)
viewModel.input(Event.Search)
advanceTimeBy(300) // For debounced operations
runCurrent()
```

**See @USF-TROUBLESHOOTING.md for comprehensive debugging guide.**

## Key Benefits

1. **Predictable** - Events always flow the same way
2. **Testable** - Pure functions, clear boundaries
3. **Efficient** - Smart lifecycle management (5s timeout before cleanup)
4. **Safe** - Thread-safe state updates via `ResultScope`
5. **Clean** - Less boilerplate than standard ViewModels

## Code Style Guidelines

```kotlin
// ✅ Single expression for simple functions
override fun initialState() = MyUiState()

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

## When to Read Specialized Guides

- **New to USF?** → Read @USF-QUICKSTART.md for complete tutorial
- **Complex features with search/pagination?** → Read @USF-PLUGINS.md
- **Writing comprehensive tests?** → Read @USF-TESTING.md
- **Debugging issues?** → Read @USF-TROUBLESHOOTING.md

---

This covers 90% of USF usage. For advanced patterns, refer to the specialized guides above.
