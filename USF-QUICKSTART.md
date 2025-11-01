# USF Quickstart Guide

> **For core patterns**, see @USF.md
> **For advanced plugins**, see @USF-PLUGINS.md
> **For comprehensive testing**, see @USF-TESTING.md
> **For troubleshooting**, see @USF-TROUBLESHOOTING.md

Start here if you're new to USF. This guide walks you through creating your first USF feature from scratch.

## What You'll Build

A simple Counter feature that demonstrates core USF concepts:
- User can increment/decrement a counter
- Shows loading state during simulated async operations
- Displays toast messages as side effects

## Prerequisites

- Understanding of Kotlin, Coroutines, and Jetpack Compose
- Familiarity with Dependency Injection concepts
- Read @USF.md for conceptual overview

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
            .padding(dp16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Count: ${uiState.count}",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(dp16))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(dp16))

        // Error message
        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(dp8))
        }

        // Buttons using state callbacks
        Row(horizontalArrangement = Arrangement.spacedBy(dp8)) {
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

        Spacer(modifier = Modifier.height(dp16))

        Button(
            onClick = uiState.onAsyncIncrement,
            enabled = !uiState.isLoading
        ) {
            Text("Async +1")
        }

        Spacer(modifier = Modifier.height(dp8))

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
- **Pipeline Management**: USF handles lifecycle automatically (5s timeout)
- **Thread Safety**: `ResultScope` ensures safe state updates

### Best Practices Applied
- Clear separation between Events, State, and Effects
- Proper error handling with loading states
- Testing with coroutine test utilities
- Dependency injection with scoped components

## Next Steps

1. **Learn Core Patterns**: Read @USF.md for common patterns and best practices
2. **Complex Features**: Check @USF-PLUGINS.md for plugin composition
3. **Comprehensive Testing**: See @USF-TESTING.md for advanced test patterns
4. **Debugging**: Refer to @USF-TROUBLESHOOTING.md when stuck

## Common Gotchas

- **Missing CoroutineScope**: USF ViewModels MUST have an injected `CoroutineScope`
- **Blocking `process()` Function**: Long operations should use `coroutineScope.launch`
- **State Mutations**: Never mutate state directly, always use `updateState { }`
- **Effect Collection**: Always use `LaunchedEffect(viewModel)` to collect effects once

For troubleshooting common issues, see @USF-TROUBLESHOOTING.md.
