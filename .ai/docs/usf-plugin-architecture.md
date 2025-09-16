# USF Plugin Architecture

> **Note**: This is an advanced USF feature. Read `.ai/docs/usf-architecture.md` first for core USF concepts.

## Quick Overview

USF Plugins let you compose complex ViewModels from reusable components. Think "feature mixins" for ViewModels.

**Without Plugins:**
```kotlin
class ProductViewModel : UsfViewModel<Event, State, Effect>() {
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
class ProductViewModel : UsfViewModel<Event, State, Effect>() {
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
interface UsfPluginInterface<Event : Any, State : Any, Effect : Any> {
    suspend fun ResultScope<State, Effect>.process(event: Event)
    fun ResultScope<State, Effect>.onSubscribed() {}
    fun onUnsubscribed() {}
}
```

Base implementation with optional internal state:

```kotlin
abstract class UsfPlugin<Event : Any, State : Any, Effect : Any> :
    UsfPluginInterface<Event, State, Effect> {
    protected val internalState = InternalStateHolder(initialInternalState())
}
```

## When to Use Plugins

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

For comprehensive testing patterns and examples, see `.ai/docs/usf-testing-guide.md`.

## Common Plugin Patterns

### Form Validation
```kotlin
class ValidationPlugin : UsfPlugin<Event, State, Effect>() {
    override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
    override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
    override suspend fun ResultScope<State, Effect>.process(event: Event) {
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
class MyViewModel : UsfViewModel<Event, State, Effect>() {
    override suspend fun process(event: Event) {
        // 200 lines of mixed logic
    }
}

// After: Composed from plugins
class MyViewModel : UsfViewModel<Event, State, Effect>() {
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