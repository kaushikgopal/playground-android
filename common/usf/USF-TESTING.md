# USF Testing Guide

> **For core patterns**, see @USF.md
> **For quickstart tutorial**, see @USF-QUICKSTART.md
> **For advanced plugins**, see @USF-PLUGINS.md
> **For troubleshooting**, see @USF-TROUBLESHOOTING.md

Comprehensive guide for testing USF ViewModels and Plugins with practical examples and best practices.

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
    coEvery { mockRepository.performOperation() } throws
        IOException("Temporary error") andThenThrows
        IOException("Still failing") andThen
        "success"

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

- Test state transitions explicitly
- Verify effects are emitted correctly
- Test error conditions and edge cases
- Use `TestScope` and `runTest` for coroutines
- Mock dependencies with specific behaviors
- Test lifecycle hooks (onSubscribed/onUnsubscribed)
- Use descriptive test names in backticks
- Separate arrange, act, and assert clearly

### DON'T

- Test internal implementation details
- Forget to call `runCurrent()` after triggering events
- Mix UI testing with ViewModel testing
- Use `GlobalScope` or real dispatchers in tests
- Create overly complex test scenarios
- Ignore cleanup in test lifecycle
- Test multiple concerns in one test

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

---

For core patterns, see @USF.md. For troubleshooting tests, see @USF-TROUBLESHOOTING.md.
