# USF (Unidirectional State Flow) Architecture

## Quick Overview

USF enforces a simple, predictable pattern for ViewModels:
**Events → Process → State/Effects**

Think of it as MVI (Model-View-Intent) with built-in lifecycle management and less boilerplate.

**Traditional ViewModel:**
```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    
    fun onButtonClick() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val result = repository.fetch()
            _state.value = _state.value.copy(loading = false, data = result)
        }
    }
}
```

**USF ViewModel:**
```kotlin
class MyViewModel : UsfViewModel<Event, State, Effect>() {
    override fun initialState() = State()
    
    override suspend fun ResultScope<State, Effect>.process(event: Event) {
        when (event) {
            is Event.ButtonClick -> {
                updateState { it.copy(loading = true) }
                val result = repository.fetch()
                updateState { it.copy(loading = false, data = result) }
            }
        }
    }
}
```

## Core Components

### The Three Types

```kotlin
// 1. Events - What the user does
sealed interface SettingsBEvent {
    data object RefreshClicked : SettingsBEvent
    data class TextChanged(val text: String) : SettingsBEvent
}

// 2. State - What the UI shows (with callbacks)
data class SettingsBUiState(
    val quoteText: String = "",
    val quoteAuthor: String = "",
    val isLoading: Boolean = false,
    // UI callbacks as part of state
    val onRefreshClick: () -> Unit = {},
    val onTextChange: (String) -> Unit = {},
)

// 3. Effects - One-time actions
sealed interface SettingsBEffect {
    data object NavigateBack : SettingsBEffect
    data class ShowToast(val message: String) : SettingsBEffect
}
```

## Real Implementation

### Basic ViewModel

```kotlin
// features/settings/SettingsBViewModelImpl.kt:14-51
@ContributesBinding(SettingsScope::class)
class SettingsBViewModelImpl @Inject constructor(
    coroutineScope: CoroutineScope,
    val quotesRepo: Lazy<QuotesRepo>,
) : UsfViewModel<SettingsBEvent, SettingsBUiState, SettingsBEffect>(
    coroutineScope = coroutineScope,
) {
    
    override fun initialState() = SettingsBUiState(
        quoteText = "Get to the CHOPPER!!!",
        quoteAuthor = "Arnold Schwarzenegger",
    )
    
    override fun ResultScope<SettingsBUiState, SettingsBEffect>.onSubscribed() {
        // Load data when UI subscribes
        coroutineScope.launch(Dispatchers.IO) {
            val quote = quotesRepo.value.quoteForTheDay()
            updateState { it.copy(
                quoteText = quote.quote,
                quoteAuthor = quote.author,
            )}
        }
    }
    
    override suspend fun ResultScope<SettingsBUiState, SettingsBEffect>.process(
        event: SettingsBEvent
    ) {
        // Handle events here
    }
}
```

### UI Integration

```kotlin
@Composable
fun SettingsBScreen(viewModel: SettingsBViewModel) {
    val uiState by viewModel.state.collectAsState()
    
    // Handle one-time effects
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsBEffect.NavigateBack -> navigator.goBack()
                is SettingsBEffect.ShowToast -> showToast(effect.message)
            }
        }
    }
    
    // UI using state
    Column {
        Text(text = uiState.quoteText)
        Text(text = "- ${uiState.quoteAuthor}")
        
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
        
        Button(onClick = uiState.onRefreshClick) {
            Text("Refresh")
        }
    }
}
```

## Key Patterns

### UI Callbacks in State

USF puts callbacks directly in the state for cleaner UI code:

```kotlin
// State with callbacks
data class LandingUiState(
    val title: String = "Landing",
    val onSettingsClick: () -> Unit = {},  // Callback in state
)

// ViewModel provides the callback
override fun initialState() = LandingUiState(
    onSettingsClick = inputCallback(LandingEvent.NavigateToSettingsClicked)
)

// UI just uses it
Button(onClick = uiState.onSettingsClick) {
    Text("Settings")
}
```

### When to Use Callbacks vs Direct Input

**Use callbacks in state:**
```kotlin
// When you need parameters from UI
val onTextChanged: (String) -> Unit
val onItemSelected: (id: Int) -> Unit

// When it's frequently called
val onScroll: (offset: Float) -> Unit
```

**Use direct viewModel.input():**
```kotlin
// Simple navigation/effects
Button(onClick = { viewModel.input(Event.NavigateBack) })

// One-time actions
LaunchedEffect(Unit) {
    viewModel.input(Event.LoadData)
}
```

### The ResultScope Magic

`ResultScope` provides two key functions inside `process()`:

```kotlin
override suspend fun ResultScope<State, Effect>.process(event: Event) {
    // 1. Update state
    updateState { currentState ->
        currentState.copy(loading = true)
    }
    
    // 2. Emit effects
    emitEffect(Effect.ShowToast("Hello"))
    
    // Both are thread-safe and scoped to the pipeline
}
```

### Lifecycle Hooks

```kotlin
// Called when first UI subscriber connects
override fun ResultScope<State, Effect>.onSubscribed() {
    loadInitialData()
}

// Called when last subscriber disconnects (5-second timeout)
override fun onUnsubscribed() {
    cleanup()
}
```

## Pipeline Lifecycle (The Smart Part)

USF ViewModels have a smart pipeline that manages resources:

1. **Dormant** - No UI observing, no resources used
2. **Active** - UI subscribed, processing events
3. **Timeout** - UI gone, 5-second countdown
4. **Cleanup** - Resources released if no UI returns

This means ViewModels automatically:
- Start when UI appears
- Pause when UI is gone
- Resume if UI returns quickly
- Clean up if UI is gone for good

## Testing

```kotlin
@Test
fun `quote loads on subscribe`() = runTest {
    val mockRepo = mockk<QuotesRepo> {
        coEvery { quoteForTheDay() } returns Quote("Test", "Author")
    }
    
    val viewModel = SettingsBViewModelImpl(
        coroutineScope = TestScope(),
        quotesRepo = Lazy { mockRepo }
    )
    
    // Start collecting (simulates UI subscription)
    val states = viewModel.state.take(2).toList()
    
    // Initial state
    assertEquals("Get to the CHOPPER!!!", states[0].quoteText)
    
    // After loading
    assertEquals("Test", states[1].quoteText)
    assertEquals("Author", states[1].quoteAuthor)
}
```

## When to Use USF

### ✅ Perfect For
- Screens with user interactions
- Complex state management
- Async operations with loading states
- Features needing predictable testing

### ❌ Not Needed For
- Simple display-only screens
- Stateless utilities
- Pure business logic (use regular classes)

## Advanced: Plugin Composition

For complex features, USF supports plugin composition (not yet used in this app):

```kotlin
// Create reusable logic
class SearchPlugin : UsfPlugin<SearchEvent, SearchState, SearchEffect>() {
    // Reusable search logic
}

// Compose into ViewModel
class ComplexViewModel : UsfViewModel<Event, State, Effect>() {
    init {
        register(SearchPlugin(), /* adapters */)
    }
}
```

**When to consider plugins:**
- Sharing logic across multiple ViewModels
- Building complex features from smaller parts
- Need isolated testing of feature components

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
    val onEmailChange: (String) -> Unit = {},
)

override suspend fun process(event: Event) {
    when (event) {
        is Event.EmailChanged -> {
            val isValid = event.email.contains("@")
            updateState { it.copy(email = event.email, isValid = isValid) }
        }
    }
}
```

### Debounced Search
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

## Migration from Standard ViewModel

```kotlin
// Before: Standard ViewModel
class OldViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    
    fun onButtonClick() {
        viewModelScope.launch {
            _state.value = state.value.copy(loading = true)
            // ...
        }
    }
}

// After: USF ViewModel
class NewViewModel : UsfViewModel<Event, State, Effect>() {
    override fun initialState() = State()
    
    override suspend fun process(event: Event) {
        when (event) {
            is Event.ButtonClick -> {
                updateState { it.copy(loading = true) }
                // ...
            }
        }
    }
}
```

## Key Benefits

1. **Predictable** - Events always flow the same way
2. **Testable** - Pure functions, clear boundaries
3. **Efficient** - Smart lifecycle management
4. **Safe** - Thread-safe state updates
5. **Clean** - Less boilerplate than standard ViewModels

USF makes complex ViewModels manageable and simple ViewModels trivial.