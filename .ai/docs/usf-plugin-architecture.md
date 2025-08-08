# USF Plugin Architecture

**Version**: 1.0.0  
**Last Updated**: 2025-08-06  
**Status**: Production Ready

## Concept

USF (Unidirectional State Flow) is a reactive architecture pattern for Android ViewModels that enforces one-way data flow with a plugin-based composition system. It provides predictable state management through Events → Processing → State/Effects, with the option to compose complex features from reusable plugin components.

## Core Components

### Essential Types
- **Event**: User actions and system inputs (sealed interface)
- **UiState**: Complete UI state at any point in time (immutable data class with UI callbacks)
- **Effect**: One-time side effects like navigation or toasts (sealed interface)
- **ResultScope**: Scoped context providing `updateState()` and `emitEffect()` functions
- **UsfViewModel**: Base class managing the event pipeline and state flow

### Plugin System (For Complex Features)
- **UsfPlugin**: Reusable logic components that can be composed into ViewModels
- **UsfPluginInterface**: Contract for plugin implementations
- **Plugin Adapters**: Type-safe converters between parent and plugin event/state/effect types

## Architecture Flow

```
User Input → Event → ViewModel/Plugin → ResultScope { 
    → updateState() → UiState → UI
    → emitEffect() → Effect → Navigation/Toast
}
```

### Pipeline Lifecycle Management
1. **Activation**: Pipeline starts when first UI subscriber begins collecting state
2. **Processing**: Events processed through channels with dedicated state dispatcher
3. **Timeout**: 5-second countdown begins when last subscriber stops
4. **Cleanup**: Pipeline shuts down and resources released if no new subscribers

## Implementation in the App

### Basic ViewModel (Most Common)

```kotlin
@Inject
class DiscoverViewModelImpl(
    coroutineScope: CoroutineScope,
) : UsfViewModel<DiscoverEvent, DiscoverUiState, DiscoverEffect>(
    coroutineScope = coroutineScope,
) {

    override fun initialState(): DiscoverUiState {
        return DiscoverUiState(
            searchText = "",
            onBackClicked = inputCallback(DiscoverEvent.BackClicked),
            onSearchTextChanged = inputCallbackWithParam(DiscoverEvent::SearchTextChanged),
        )
    }

    override suspend fun ResultScope<DiscoverUiState, DiscoverEffect>.process(event: DiscoverEvent) {
        when (event) {
            is DiscoverEvent.BackClicked -> {
                emitEffect(DiscoverEffect.NavigateBack)
            }
            is DiscoverEvent.SearchTextChanged -> {
                updateState { it.copy(searchText = event.text) }
                if (event.text.length >= 3) {
                    performSearch(event.text)
                }
            }
        }
    }

    private suspend fun ResultScope<DiscoverUiState, DiscoverEffect>.performSearch(query: String) {
        updateState { it.copy(isLoading = true) }
        // Perform async search operation
        val results = searchRepository.search(query)
        updateState { it.copy(isLoading = false, results = results) }
    }
}
```

### Domain Models

```kotlin
// Events - User actions
sealed interface DiscoverEvent {
    data object BackClicked : DiscoverEvent
    data class SearchTextChanged(val text: String) : DiscoverEvent
}

// UI State - Complete UI representation
data class DiscoverUiState(
    val searchText: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    // UI Callbacks - Functions the UI can invoke
    val onBackClicked: () -> Unit,
    val onSearchTextChanged: (String) -> Unit,
)

// Effects - One-time side effects
sealed interface DiscoverEffect {
    data object NavigateBack : DiscoverEffect
    data class ShowError(val message: String) : DiscoverEffect
}
```

### UI Integration

```kotlin
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()
    
    // Handle effects
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiscoverEffect.NavigateBack -> navController.navigateUp()
                is DiscoverEffect.ShowError -> showSnackbar(effect.message)
            }
        }
    }
    
    Column(modifier) {
        TextField(
            value = uiState.searchText,
            onValueChange = uiState.onSearchTextChanged,
            label = { Text("Search") }
        )
        
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(uiState.results) { result ->
                    SearchResultItem(result)
                }
            }
        }
    }
}
```

## Advanced: Plugin Composition

For complex features with reusable logic, compose ViewModels from plugins:

### Creating a Plugin

```kotlin
class SearchPlugin(
    initialScope: CoroutineScope,
    private val searchRepository: SearchRepository,
) : UsfPlugin<SearchEvent, SearchState, SearchEffect>(
    initialScope = initialScope,
) {

    override fun initialState() = SearchState()

    override suspend fun ResultScope<SearchState, SearchEffect>.process(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                updateState { it.copy(query = event.query) }
                if (event.query.length >= 3) {
                    searchDebounced(event.query)
                }
            }
            is SearchEvent.Clear -> {
                updateState { SearchState() }
            }
        }
    }
}
```

### Composing Plugins into ViewModel

```kotlin
class ComplexViewModel(
    coroutineScope: CoroutineScope,
) : UsfViewModel<ComplexEvent, ComplexUiState, ComplexEffect>(
    coroutineScope = coroutineScope,
) {
    
    private val searchPlugin = SearchPlugin(coroutineScope, searchRepository)
    private val filterPlugin = FilterPlugin(coroutineScope)
    
    init {
        // Register search plugin with adapters
        register(
            plugin = searchPlugin,
            adaptEvent = { parentEvent ->
                when (parentEvent) {
                    is ComplexEvent.Search -> SearchEvent.QueryChanged(parentEvent.query)
                    is ComplexEvent.ClearSearch -> SearchEvent.Clear
                    else -> null
                }
            },
            adaptState = { pluginState, parentState ->
                parentState.copy(
                    searchQuery = pluginState.query,
                    searchResults = pluginState.results,
                )
            },
            adaptEffect = { pluginEffect ->
                when (pluginEffect) {
                    is SearchEffect.SearchComplete -> ComplexEffect.RefreshUI
                    else -> null
                }
            }
        )
        
        // Register filter plugin
        register(filterPlugin, /* adapters */)
    }
}
```

## Key Patterns

### UI Callbacks vs Direct Events

#### When to Avoid inputCallback (Recommended)
For simple navigation and effects, prefer calling `viewModel.input()` directly from the UI:

```kotlin
// ✅ PREFERRED: Direct event dispatch for simple effects
@Composable
fun MyScreen(viewModel: MyViewModel) {
    Button(onClick = { viewModel.input(MyEvent.NavigateClicked) }) {
        Text("Navigate")
    }
}

// In ViewModel - Simple, no state needed
data class MyUiState(
    val title: String = "My Screen",
    // No callback needed for navigation
)
```

#### When to Use inputCallback
Use callbacks when the UI state needs to provide functions that encapsulate complex logic or when state values need to be captured:

```kotlin
// ✅ USE CALLBACKS: When state values are involved
data class MyUiState(
    val searchText: String = "",
    val onSearchTextChanged: (String) -> Unit,  // Needs the text parameter
    val onItemSelected: (id: Int, selected: Boolean) -> Unit,  // Multiple parameters
)

override fun initialState() = MyUiState(
    onSearchTextChanged = inputCallbackWithParam(MyEvent::SearchTextChanged),
    onItemSelected = { id, selected -> 
        input(MyEvent.ItemSelected(id, selected))
    },
)
```

**Guidelines:**
- **Simple effects** (navigation, showing toasts): Use direct `viewModel.input()` calls
- **State-dependent actions**: Use callbacks in UiState
- **Forms and inputs**: Use callbacks for onChange handlers
- **Pure navigation buttons**: Avoid callbacks, use direct event dispatch

### State Updates
Always use immutable updates within ResultScope:
```kotlin
updateState { currentState ->
    currentState.copy(
        isLoading = true,
        error = null,
    )
}
```

### Async Operations
Launch coroutines within the process function:
```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadData -> {
            updateState { it.copy(isLoading = true) }
            
            try {
                val data = repository.fetchData()
                updateState { it.copy(isLoading = false, data = data) }
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false, error = e.message) }
                emitEffect(Effect.ShowErrorToast(e.message))
            }
        }
    }
}
```

## Migration Guide

### From Standard ViewModel
1. Extend `UsfViewModel<Event, UiState, Effect>` instead of `ViewModel()`
2. Define Event, UiState, and Effect sealed types
3. Move all UI callbacks into UiState data class
4. Implement `initialState()` and `process()` functions
5. Replace `viewModelScope.launch` with pipeline-managed coroutines

### Adding Plugin Support
1. Identify reusable logic that could be extracted
2. Create `UsfPlugin` implementation with its own Event/State/Effect
3. Register plugin in ViewModel's `init` block with adapters
4. Test plugin independently from ViewModel

## Best Practices

### DO
- Keep Events focused on user intent, not implementation
- Make UiState a complete representation of the UI
- Use Effects only for one-time actions
- Test ViewModels with the provided test utilities
- Use plugins for genuinely reusable logic

### DON'T
- Don't expose mutable state outside ViewModels
- Don't process events outside ResultScope
- Don't create plugins for single-use logic
- Don't forget to handle errors in async operations
- Don't use GlobalScope - use injected CoroutineScope

## Testing

```kotlin
@Test
fun `search updates state correctly`() = runTest {
    val viewModel = DiscoverViewModelImpl(
        coroutineScope = TestScope(),
    )
    
    viewModel.test { input, states, effects ->
        // Initial state
        assertThat(states.first().searchText).isEmpty()
        
        // User types
        input(DiscoverEvent.SearchTextChanged("kotlin"))
        
        // State updated
        assertThat(states.last().searchText).isEqualTo("kotlin")
    }
}
```

## Architecture Benefits

- **Predictable**: Single source of truth for state
- **Testable**: Pure functions and clear boundaries
- **Composable**: Build complex features from simple plugins
- **Efficient**: Pipeline only active when UI is observing
- **Safe**: Automatic cleanup and error handling
- **Maintainable**: Clear separation of concerns