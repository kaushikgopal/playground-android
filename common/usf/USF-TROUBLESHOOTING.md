# USF Troubleshooting Guide

> **For core patterns**, see @USF.md
> **For quickstart tutorial**, see @USF-QUICKSTART.md
> **For advanced plugins**, see @USF-PLUGINS.md
> **For comprehensive testing**, see @USF-TESTING.md

Common issues and their solutions when working with USF ViewModels and Plugins.

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
// Wrong - don't use viewModelScope
class MyViewModel : ViewModel() {
    private val usfViewModel = UsfViewModel<Event, UiState, Effect>(viewModelScope)
}

// Correct - inject CoroutineScope
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
// Wrong
@Test
fun `test state update`() = runTest {
    viewModel.input(Event.Update)
    // Missing runCurrent() - state won't update
    assertThat(viewModel.state.value.updated).isTrue()
}

// Correct
@Test
fun `test state update`() = runTest {
    viewModel.input(Event.Update)
    runCurrent() // Process the event
    assertThat(viewModel.state.value.updated).isTrue()
}
```

#### 2. State Updates in Wrong Context

```kotlin
// Wrong - updateState outside ResultScope
override suspend fun process(event: Event) {
    coroutineScope.launch {
        updateState { it.copy(loading = false) } // Won't work!
    }
}

// Correct - updateState in ResultScope
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    coroutineScope.launch {
        val data = repository.fetch()
        updateState { it.copy(data = data) } // Works!
    }
}
```

#### 3. Mutating State Instead of Copying

```kotlin
// Wrong - mutating existing state
data class MyState(val items: MutableList<Item> = mutableListOf())

updateState { state ->
    state.items.add(newItem) // Mutates existing state!
    state // Returns same reference - no recomposition
}

// Correct - creating new state
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
// Wrong - effects not collected
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.state.collectAsState()
    // Missing effect collection!

    MyContent(uiState)
}

// Correct - collecting effects
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
// Wrong - effect collection restarts on every state change
LaunchedEffect(uiState) {
    viewModel.effects.collect { /* handle */ }
}

// Correct - effect collection stable across state changes
LaunchedEffect(viewModel) {
    viewModel.effects.collect { /* handle */ }
}
```

### Tests Hanging or Failing

#### 1. Not Using TestScope

```kotlin
// Wrong - real coroutine scope in tests
val viewModel = MyViewModelImpl(
    coroutineScope = CoroutineScope(Dispatchers.IO)
)

// Correct - TestScope for controlled execution
val viewModel = MyViewModelImpl(
    coroutineScope = TestScope().backgroundScope
)
```

#### 2. Not Advancing Time for Delays

```kotlin
// Wrong - real delays in tests
@Test
fun `search with debounce`() = runTest {
    viewModel.input(SearchEvent.Query("test"))
    delay(500) // Real delay - test becomes slow
    assertThat(viewModel.state.value.results).isNotEmpty()
}

// Correct - advance test time
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
// Wrong - mock doesn't return anything
coEvery { mockRepository.fetch() } returns Unit // Wrong return type

// Correct - proper mock setup
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
// Wrong - job continues after unsubscribe
private var longRunningJob: Job? = null

override fun ResultScope<UiState, Effect>.onSubscribed() {
    longRunningJob = coroutineScope.launch {
        while (true) {
            delay(1000)
            // This will continue running!
        }
    }
}

// Correct - cancel job on unsubscribe
override fun onUnsubscribed() {
    longRunningJob?.cancel()
    longRunningJob = null
}
```

#### 2. External References Holding ViewModel

```kotlin
// Wrong - static reference prevents GC
object GlobalCache {
    var viewModel: MyViewModel? = null // Holds reference!
}

// Correct - use WeakReference or clear explicitly
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
// Wrong - updating state on every character
override suspend fun ResultScope<UiState, Effect>.process(event: SearchEvent.TextChanged) {
    repository.searchItems(event.text) // Network call on every character!
        .collect { results ->
            updateState { it.copy(results = results) }
        }
}

// Correct - debounce updates
private var searchJob: Job? = null

override suspend fun ResultScope<UiState, Effect>.process(event: SearchEvent.TextChanged) {
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
// Wrong - lambda recreated on each state update
data class UiState(
    val onItemClick: (Item) -> Unit = { item ->
        // This lambda is recreated every time!
        processItem(item)
    }
)

// Correct - stable callbacks or direct input
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

1. All required imports present
2. CoroutineScope properly injected
3. ResultScope used in process function
4. Effects collected in UI
5. Tests use TestScope and runCurrent()

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

---

For core patterns, see @USF.md. For comprehensive testing patterns, see @USF-TESTING.md.
