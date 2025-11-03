# USF Phase 1: Main-Thread-First Architecture

This ExecPlan follows .ai/plans/PLANS.md requirements and is a living document.

Last Updated: 2025-11-02T00:00:00Z

---

# DESIGN

## Purpose / Big Picture

This change fixes TextField input latency by eliminating unnecessary thread hops in the USF (Unidirectional State Flow) architecture. Currently, every state update involves 3 context switches (Main → IO → State → Main), adding 20-30μs of overhead and causing noticeable lag during rapid text input.

After this change, simple UI updates (like typing in a TextField) will execute entirely on the main thread with zero context switches, reducing overhead to ~6μs. Heavy operations (database queries, network calls, CPU-intensive work) will use an explicit `offload` helper function to move work to background threads.

**Observable outcome:** Add or use any screen with a TextField bound to ViewModel state (e.g., temporarily add one to Settings A). Type rapidly and observe instant character appearance with no lag. No complex "sync TextField ↔ ViewModel" workaround should be needed.

**User-visible value:** Better app responsiveness, especially during text input and other rapid UI interactions.

## Context & Orientation

### Current Architecture

USF ViewModels currently default to processing events on `Dispatchers.IO`:

```kotlin
abstract class UsfViewModel<Event, UiState, Effect>(
    coroutineScope: CoroutineScope,
    private val processingDispatcher: CoroutineDispatcher = Dispatchers.IO,
)
```

This causes 3 thread hops for every state update:
1. Main thread → Event sent to channel
2. IO thread → Event processed via `.flowOn(processingDispatcher)`
3. State thread → State updated via `withContext(stateDispatcher)`
4. Main thread → StateFlow emits, UI recomposes

The `processingDispatcher` was intended to prevent blocking the main thread, but in practice:
- 90% of ViewModel operations are simple state copies (instant)
- Heavy operations should be explicitly offloaded (not automatic)
- The automatic offloading adds latency to operations that don't need it

### Problem Evidence

In the reference repo, a complex TextField synchronization workaround was necessary due to USF thread hops. In playground-android, such a workaround does not exist yet; we will validate the improvement by adding a temporary TextField bound directly to ViewModel state and observing responsiveness during rapid input.

### Key Files (playground-android)

- `common/usf/src/main/java/sh/kau/playground/usf/viewmodel/UsfViewModel.kt` — Main USF ViewModel (351 lines)
- `common/usf/src/main/java/sh/kau/playground/usf/plugin/UsfPlugin.kt` — USF Plugin base
- `common/usf/src/main/java/sh/kau/playground/usf/scope/ResultScope.kt` — ResultScope API
- `features/settings/impl/src/main/java/sh/kau/playground/features/settings/ui/SettingsAScreen.kt` — Good place to add a temporary TextField for manual validation
- `features/landing/src/main/java/sh/kau/playground/landing/ui/LandingViewModelImpl.kt` — Simple USF VM example (no TextField yet)
- `common/usf/src/test/java/sh/kau/playground/usf/impl/UsfViewModelTest.kt` — Comprehensive tests

### Technical Terms

- **USF (Unidirectional State Flow):** Architecture pattern where Events → Process → State/Effects flows in one direction
- **ResultScope:** Context object providing `updateState()` and `emitEffect()` functions inside event processing
- **processingDispatcher:** CoroutineDispatcher determining which thread events are processed on
- **stateDispatcher:** Serialized dispatcher (limitedParallelism(1)) for thread-safe state updates
- **Pipeline:** Flow-based event processing system that manages subscription lifecycle

## Plan of Work

### Phase 1 Architecture Changes

Core principle: Process events on the main thread (Dispatchers.Main.immediate) by default; explicitly offload heavy work via `offload {}` to a background dispatcher. Inspector work remains off-main.

Four main changes:

1) Use viewModelScope on Main.immediate for event processing
   - Remove `.flowOn(processingDispatcher)` from the pipeline
   - Remove `withContext(processingDispatcher)` from event processing
   - Make `Dispatchers.Main.immediate` explicit on launches that touch state/effects

2) Remove redundant state dispatcher and add a main fast‑path
   - Delete `stateDispatcher = processingDispatcher.limitedParallelism(1)`
   - Remove `withContext(stateDispatcher)` from `updateState`
   - If already on Main.immediate, apply `_state.update(update)` synchronously; otherwise, launch to main with the handler

3) Add `offload` helper to ResultScope for explicit background work
   - New function: `suspend fun <T> offload(block: suspend () -> T): T`
   - Runs `block` on `processingDispatcher` and returns to the caller’s context (typically main)

4) Move inspector to background
   - Inspector calls use `launch(processingDispatcher)` (fire‑and‑forget)
   - Remove `inspectEvent`/`inspectEffect` helpers and inline inspection behind background launches

### Implementation Order

1. Update `ResultScope` interface with `offload` function
2. Modify `UsfViewModel` implementation (main-fast-path + Main.immediate)
3. Apply same changes to `UsfPlugin` for consistency
4. Add debug-only StrictMode gate in the app module
5. Run full test suite - should pass without changes
6. Manual test TextField responsiveness (e.g., with a temporary field on Settings A)
7. Cleanup: remove temporary field or keep as an example if desired
8. Update documentation with offload + main-fast-path notes
9. Add lint rule to detect blocking operations without offload

## Concrete Steps

### Step 1: Update ResultScope Interface

**File:** `common/usf/src/main/java/sh/kau/playground/usf/scope/ResultScope.kt`

Add `offload` function to interface:

```kotlin
interface ResultScope<State, Effect> {
    fun updateState(update: (State) -> State)
    fun emitEffect(effect: Effect)

    /**
     * Offloads heavy work to a background dispatcher and returns the result.
     *
     * This is a convenience helper for common cases. For advanced scenarios requiring
     * fine-grained control over dispatchers, cancellation, or parallel execution,
     * use the underlying coroutines primitives directly (withContext, async/await, etc.).
     *
     * Use this for CPU-intensive or I/O operations that should not block the main thread.
     * The block runs on a background dispatcher, then returns the result back to the
     * calling context (typically main thread).
     *
     * Example:
     * ```kotlin
     * override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
     *     when (event) {
     *         is Event.LoadFeeds -> {
     *             updateState { it.copy(loading = true) }
     *
     *             val feeds = offload {
     *                 database.fetchFeeds() // Runs on IO thread
     *             }
     *             // Automatically back on main thread here
     *
     *             updateState { it.copy(loading = false, feeds = feeds) }
     *         }
     *     }
     * }
     * ```
     *
     * @param block The suspend function to execute on a background dispatcher
     * @return The result from executing the block
     */
    suspend fun <T> offload(block: suspend () -> T): T
}
```

**Expected:** Interface compiles, no runtime changes yet.

### Step 2: Update UsfViewModel Implementation

**File:** `common/usf/src/main/java/sh/kau/playground/usf/viewmodel/UsfViewModel.kt`

**Change 2a: Remove stateDispatcher (line 73)**

Delete:
```kotlin
private val stateDispatcher = processingDispatcher.limitedParallelism(1)
```

**Change 2b: Update ResultScope implementation (lines 141–148) with main fast‑path**

Before:
```kotlin
private val resultScope =
    object : ResultScope<UiState, Effect> {
      override fun updateState(update: (UiState) -> UiState) {
        _pipelineScope?.launch(handler) { withContext(stateDispatcher) { _state.update(update) } }
      }

      override fun emitEffect(effect: Effect) {
        emit(effect)
      }
    }
```

After (explicit Main.immediate + fast‑path):
```kotlin
private val resultScope =
    object : ResultScope<UiState, Effect> {
      override fun updateState(update: (UiState) -> UiState) {
        // If already on Main.immediate, update synchronously to minimize latency.
        // Otherwise, post to main via the handler‑wrapped launch for safety and consistency.
        val scope = _pipelineScope ?: _viewModelScope
        if (!Dispatchers.Main.immediate.isDispatchNeeded(scope.coroutineContext)) {
          _state.update(update)
        } else {
          scope.launch(handler + Dispatchers.Main.immediate) { _state.update(update) }
        }
      }

      override fun emitEffect(effect: Effect) {
        emit(effect)
      }

      override suspend fun <T> offload(block: suspend () -> T): T {
        return withContext(processingDispatcher) { block() }
      }
    }
```

**Change 2c: Update pipeline to remove flowOn (line 156) and ensure Main.immediate**

Before:
```kotlin
private val pipeline =
    eventsChannel
        .receiveAsFlow()
        .onStart { _inspector?.onPipelineStarted() }
        .onCompletion { _inspector?.onPipelineStopped() }
        .flowOn(processingDispatcher)
        .onEach { event ->
```

After:
```kotlin
private val pipeline =
    eventsChannel
        .receiveAsFlow()
        .onStart { _inspector?.onPipelineStarted() }
        .onCompletion { _inspector?.onPipelineStopped() }
        .onEach { event ->
```

**Change 2d: Update event processing (lines 157–167)**

Before:
```kotlin
.onEach { event ->
  _pipelineScope?.launch(handler) {
    inspectEvent(event)
    try {
      withContext(processingDispatcher) { resultScope.run { process(event) } }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      _inspector?.error(e, "[ev → s|e]")
    }
  }
}
```

After:
```kotlin
        .onEach { event ->
          _pipelineScope?.launch(handler) {
            // Fire-and-forget inspector logging on background thread
            launch(processingDispatcher) {
              _inspector?.onEvent(event)
            }

    try {
      resultScope.run { process(event) }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      _inspector?.error(e, "[ev → s|e]")
    }
          }
        }
```

Additionally, ensure any launches that touch state/effects are declared with `Dispatchers.Main.immediate`.

**Change 2e: Update effect emission (lines 317–321)**

Before:
```kotlin
private fun emit(effect: Effect) {
  _viewModelScope.launch(handler) {
    _effects.send(effect)
    inspectEffect(effect)
  }
}
```

After (explicit Main.immediate for the emission launch):
```kotlin
private fun emit(effect: Effect) {
  _viewModelScope.launch(handler + Dispatchers.Main.immediate) {
    _effects.send(effect)

    // Fire-and-forget inspector logging on background thread
    launch(processingDispatcher) {
      _inspector?.onEffect(effect)
    }
  }
}
```

**Change 2f: Delete inspector helper functions (lines 325–333)**

Delete these two functions entirely:
```kotlin
private suspend fun inspectEvent(event: Any) {
  withContext(processingDispatcher) { _inspector?.onEvent(event) }
}

private suspend fun inspectEffect(effect: Effect) {
  withContext(processingDispatcher) { _inspector?.onEffect(effect) }
}
```

**Expected:** UsfViewModel compiles and runs on main thread by default. Tests should pass.

### Step 3: Update UsfPlugin Implementation

**File:** `common/usf/src/main/java/sh/kau/playground/usf/plugin/UsfPlugin.kt`

Apply the same changes as UsfViewModel:
- Remove any `stateDispatcher` if present
- Update ResultScope implementation to add `offload`
- Remove `withContext(processingDispatcher)` from event processing
- Update inspector calls to use `launch(processingDispatcher)`
- Remove inspector helper functions

Exact line numbers may differ, but patterns are identical to UsfViewModel changes.

Details for playground-android plugin:
- `updateState` currently updates state inline and logs via a launched coroutine. Replace with a main-fast‑path similar to ViewModel: apply synchronously when already on `Dispatchers.Main.immediate`, otherwise `launch(handler + Dispatchers.Main.immediate) { ... }`. Keep inspector logging as fire‑and‑forget on `processingDispatcher`.
- `input(event)` currently launches and does `withContext(processingDispatcher) { process(event) }`. Change to launch with `handler + Dispatchers.Main.immediate` and call `resultScope.run { process(event) }` directly. Move event inspection to `launch(processingDispatcher)`.
- Effects: emit on main (`launch(handler + Dispatchers.Main.immediate)`), and move inspector effect logging to `launch(processingDispatcher)`.

**Expected:** UsfPlugin compiles and behaves consistently with UsfViewModel.

### Step 4: Add Debug‑only StrictMode Gate

Add a small initializer to enable StrictMode in debug builds so any accidental disk/network/long operations on main are surfaced during development.

Implementation sketch (playground-android paths):
- File: `app/src/debug/java/sh/kau/playground/app/StrictModeInitializer.kt`
  - Expose `fun enableStrictMode() { StrictMode.setThreadPolicy(...); StrictMode.setVmPolicy(...) }`
- Call from Application `onCreate()` when `BuildConfig.DEBUG` (file: `app/src/main/java/sh/kau/playground/app/AppImpl.kt`)
- Policies: detect disk reads/writes, network, custom slow calls; penalties: at least `penaltyLog()`; consider `penaltyDeathOnNetwork()` in debug if acceptable

This gate is active only in debug builds and has no runtime effect in release.

### Step 5: Run Test Suite

**Command:** `make tests`

**Expected output:**
- All 25+ tests in UsfViewModelTest pass
- No changes to test code needed (tests already use TestDispatcher)
- Output should show green/passing tests

If tests fail, check:
1. Did you preserve `launch(handler)` wrapper in updateState?
2. Did you keep the pipeline lifecycle management intact?
3. Did you accidentally remove exception handling?

### Step 6: Manual Testing - TextField Responsiveness

Option A (recommended):
- Temporarily add a TextField to `features/settings/impl/src/main/java/sh/kau/playground/features/settings/ui/SettingsAScreen.kt` bound to a new `uiState.sampleText` and `uiState.onSampleTextChanged` (created via `inputEventCallback`).
- Build and run, type rapidly, observe responsiveness.

Option B:
- Use any existing screen with a TextField bound to ViewModel state.

Expected behavior for both options:
- Each character appears instantly as typed
- No lag or delay between keypress and display
- Cursor position updates immediately
- No stuttering or frame drops

Acceptance criteria:
- TextField feels as responsive as native Android TextField
- No noticeable latency compared to other apps
- Typing speed limited only by human input, not by processing

### Step 7: Cleanup (if you added a temporary TextField)

- Either remove the temporary field or convert it into a proper example once validated.

**Test again:** Type rapidly, verify responsiveness maintained with simpler code.

### Step 8: Update Documentation

Update the following files with offload examples:

**File:** `USF.md`

Add section after line 160 (in "Basic ViewModel Implementation" section):

```markdown
### Offloading Heavy Work

For operations that should not block the main thread (network calls, database queries, CPU-intensive work), use the `offload` helper:

```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadFeeds -> {
            updateState { it.copy(loading = true) }

            // Offload runs on background thread, returns result to main
            val feeds = offload {
                repository.fetchFeeds() // Heavy I/O operation
            }

            updateState { it.copy(loading = false, feeds = feeds) }
        }

        is Event.ProcessImage -> {
            val processed = offload {
                imageProcessor.process(event.image) // CPU-intensive
            }
            updateState { it.copy(processedImage = processed) }
        }
    }
}
```

For advanced scenarios requiring fine-grained control (parallel execution, custom dispatchers, etc.), use coroutines directly:

```kotlin
override suspend fun ResultScope<UiState, Effect>.process(event: Event) {
    when (event) {
        is Event.LoadDashboard -> {
            // Parallel execution with async/await
            val (user, feeds) = coroutineScope {
                val userDeferred = async(Dispatchers.IO) { userRepo.getUser() }
                val feedsDeferred = async(Dispatchers.IO) { feedRepo.getFeeds() }
                Pair(userDeferred.await(), feedsDeferred.await())
            }
            updateState { it.copy(user = user, feeds = feeds) }
        }
    }
}
```
```

**File:** `USF-QUICKSTART.md`

Update Step 3 example (lines 36-73) to show offload usage in async increment:

```kotlin
is CounterEvent.AsyncIncrement -> {
    updateState { it.copy(isLoading = true, errorMessage = null) }

    try {
        // Simulate async operation with offload
        val newValue = offload {
            delay(1000)
            repository.incrementAsync(state.value.count)
        }
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
```

**File:** `AGENTS.md`

Update USF section (around line 70) to mention main-thread-first approach:

```markdown
## Unidirectional State Flow (USF)
- Every screen uses `UsfViewModel<Event, UiState, Effect>` with immutable state and one-off effects
- Events process on the main thread (`Dispatchers.Main.immediate`) by default for instant UI updates
- Heavy operations (network, database, CPU-intensive) use `offload { }` helper to run on background threads
- Events flow through `process` inside `ResultScope`; state updates via `updateState { … }` (fast‑path on main), side effects via `emitEffect`
- Debug builds enable StrictMode to catch blocking work on main
```

## Interfaces & Dependencies

### ResultScope Interface

```kotlin
interface ResultScope<State, Effect> {
    /**
     * Updates the current state using the provided update function.
     * Runs on main thread - state updates should be fast (simple data copies).
     */
    fun updateState(update: (State) -> State)

    /**
     * Emits a one-time effect.
     */
    fun emitEffect(effect: Effect)

    /**
     * Offloads heavy work to a background dispatcher.
     * Convenience helper for common cases. For advanced control, use coroutines directly.
     */
    suspend fun <T> offload(block: suspend () -> T): T
}
```

### Dependencies

No new dependencies required. Uses existing:
- `kotlinx.coroutines.CoroutineDispatcher`
- `kotlinx.coroutines.withContext`
- `kotlinx.coroutines.flow.Flow`
Additionally in the app module (debug only): `android.os.StrictMode`.

### Threading Model

**Before:**
```
Event (Main) → Channel → IO Thread → Process → State Thread → Update → Main Thread
```

**After:**
```
Event (Main) → Channel → Main Thread → Process → Update → Main Thread
                                            ↓
                              offload { } → IO Thread → Return to Main
```

## Validation & Acceptance

### Automated Tests

**Command:** `make tests`

**Criteria:**
- All 25+ tests in `UsfViewModelTest` pass
- Specific tests to verify:
  - `initialViewStateEmittedOnSubscription` - Initial state still emitted
  - `input_StateChangeAndEffect` - Events process and update state
  - `processMultipleEvents_InSequence` - Sequential processing maintained
  - `pipelineTerminationAndRestart` - Lifecycle management intact
  - `asyncStateUpdate_CompletesAndUpdatesState` - Async operations still work
  - `onSubscribedJobsCanceledOnPipelineTermination` - Job cleanup works

### Manual Acceptance Tests

**Test 1: TextField Responsiveness**
- Action: Add a temporary TextField to `SettingsAScreen` bound to `uiState.someText` and `uiState.onSomeTextChanged` (created via `inputEventCallback`). Type rapidly.
- Expected: Each character appears instantly with no lag
- Pass criteria: Indistinguishable from native Android TextField

**Test 2: Heavy Operation Offloading**
- Action: Trigger feed refresh (implement example if needed)
- Expected: UI remains responsive during fetch, no ANR
- Pass criteria: Can interact with UI while background work happens

**Test 3: Error Handling**
- Action: Trigger operation that throws exception
- Expected: Error caught, state updated, effect emitted
- Pass criteria: App doesn't crash, user sees error message

**Test 4: Configuration Change**
- Action: Rotate device during operation
- Expected: State preserved, operation continues or restarts appropriately
- Pass criteria: No crashes, state consistency maintained

### Performance Metrics

Measure TextField input latency (relative):
- Before: Multiple thread hops per character (Main → IO → State → Main)
- After: Zero thread hops for state-only updates (Main only)
- Target: 3-5x improvement, imperceptible to user

### Code Review Checklist

- [ ] All thread hops removed from main processing path
- [ ] Inspector calls moved to background (fire-and-forget)
- [ ] `offload` function properly implemented
- [ ] `Dispatchers.Main.immediate` used explicitly for UI-touching launches
- [ ] Documentation updated with clear examples
- [ ] No blocking operations in example `process()` functions
- [ ] Error handling preserved
- [ ] Pipeline lifecycle management intact
- [ ] Tests passing with no modifications

---

# EXECUTION

## Current Status

Not started. This plan is adapted to playground-android and ready for execution.

## Decision Log

### Decision: Keep processingDispatcher parameter
**Date:** 2025-01-02T00:00:00Z
**Rationale:** Allows flexibility to override dispatcher for specific ViewModels or plugins. Default changes to Main.immediate, but option to customize remains. Used exclusively for background work (inspector, offload helper).
**Files Affected:** UsfViewModel.kt, UsfPlugin.kt

### Decision: Make Main.immediate explicit on UI-touching launches
**Date:** 2025-01-02T00:00:00Z
**Rationale:** Declaring `Dispatchers.Main.immediate` removes ambiguity about execution context and avoids extra posts when already on main.
**Files Affected:** UsfViewModel.kt (state/effects emission, pipeline), UsfPlugin.kt

### Decision: Add main fast‑path for state updates
**Date:** 2025-01-02T00:00:00Z
**Rationale:** For ultra‑hot UI paths (e.g., TextField), applying `_state.update` synchronously when already on main minimizes latency while preserving safety when invoked from offloaded coroutines.
**Files Affected:** UsfViewModel.kt (ResultScope.updateState), UsfPlugin.kt (ResultScope.updateState)

### Decision: Add debug‑only StrictMode gate
**Date:** 2025-01-02T00:00:00Z
**Rationale:** Provides guardrails to catch accidental main‑thread blocking operations during development without affecting release builds.
**Files Affected:** app module (Application initialization)

### Decision: Use fire-and-forget for inspector calls
**Date:** 2025-01-02T00:00:00Z
**Rationale:** Inspector logging should never block event processing. Using `launch(processingDispatcher)` without waiting allows inspection to happen asynchronously on background thread. If inspector throws exception, it's caught by handler but doesn't affect event processing.
**Files Affected:** UsfViewModel.kt (pipeline and emit functions)

### Decision: Keep launch(handler) wrapper in updateState
**Date:** 2025-01-02T00:00:00Z
**Rationale:** Even though we're on main thread, the launch wrapper provides exception handling and ensures consistent dispatcher context if updateState is called from an offloaded coroutine. Minimal performance cost (~1μs) for important safety guarantees.
**Files Affected:** UsfViewModel.kt line ~142

### Decision: Add convenience helper rather than force raw coroutines
**Date:** 2025-01-02T00:00:00Z
**Rationale:** The `offload` helper makes the common case (sequential background work) simple and readable. Advanced cases can still use raw coroutines (async/await, custom dispatchers, etc.). This strikes a balance between simplicity and power. Documentation notes that offload is a convenience helper and raw coroutines are available for advanced scenarios.
**Files Affected:** ResultScope.kt, UsfViewModel.kt

## Surprises & Discoveries

(To be filled during implementation)

## Next Steps

1. Get user approval for plan
2. Implement Step 1: Update ResultScope interface
3. Implement Step 2: Update UsfViewModel
4. Implement Step 3: Update UsfPlugin
5. Run test suite and verify all tests pass
6. Manual test TextField responsiveness
7. Remove workaround if successful
8. Update documentation

## Outcomes & Retrospective

(To be filled after completion)

---

# TASKS

## Completed

(None yet - awaiting approval)

## In Progress

- [ ] Planning and design phase
  - [x] Deep analysis of current architecture
  - [x] Design Phase 1 changes
  - [x] Write ExecPlan
  - [ ] Get user approval

## Pending

### Core Implementation
- [ ] Update ResultScope interface with offload function
  - [ ] Add function signature to `common/usf/src/main/java/sh/kau/playground/usf/scope/ResultScope.kt`
  - [ ] Add comprehensive doc comment with examples
  - [ ] Verify interface compiles

- [ ] Update UsfViewModel.kt implementation (`common/usf/src/main/java/sh/kau/playground/usf/viewmodel/UsfViewModel.kt`)
  - [ ] Remove `stateDispatcher` variable (line 73)
  - [ ] Update ResultScope implementation (lines 141–148)
    - [ ] Remove withContext from updateState
    - [ ] Add offload implementation
    - [ ] Add main fast‑path for updateState when already on Main.immediate
  - [ ] Remove `.flowOn(processingDispatcher)` from pipeline (line 156)
  - [ ] Update event processing (lines 157–167)
    - [ ] Remove `withContext(processingDispatcher)`
    - [ ] Add `launch(processingDispatcher)` for inspector
    - [ ] Add `Dispatchers.Main.immediate` to the launch that runs process
  - [ ] Make `Dispatchers.Main.immediate` explicit for UI-touching launches
  - [ ] Update `emit` function (lines 317–321)
    - [ ] Add `launch(processingDispatcher)` for inspector
  - [ ] Delete `inspectEvent` (lines 325–327)
  - [ ] Delete `inspectEffect` (lines 330–331)

- [ ] Update UsfPlugin.kt implementation (`common/usf/src/main/java/sh/kau/playground/usf/plugin/UsfPlugin.kt`)
  - [ ] Add main fast‑path in `updateState`
  - [ ] Add `offload` implementation in ResultScope
  - [ ] Process events on Main.immediate; move inspector to background
  - [ ] Emit effects on Main.immediate; move inspector to background
  - [ ] Verify consistency with ViewModel behavior

- [ ] Add debug-only StrictMode gate in app module
  - [ ] Add `app/src/debug/java/sh/kau/playground/app/StrictModeInitializer.kt`
  - [ ] Call from `app/src/main/java/sh/kau/playground/app/AppImpl.kt` when debuggable
  - [ ] Detect disk IO, network, and slow calls on main; penalties: at least `penaltyLog()`

### Testing
- [ ] Run full test suite
  - [ ] Execute `make tests`
  - [ ] Verify all 25+ tests pass
  - [ ] Debug any failures

- [ ] Manual testing
- [ ] Test TextField responsiveness (e.g., temporary field in Settings A)
  - [ ] Test heavy operations (if examples exist)
  - [ ] Test error handling
  - [ ] Test configuration changes

- [ ] Performance verification
  - [ ] Measure TextField input latency improvement
  - [ ] Verify no frame drops during text input

### Cleanup
- [ ] If a temporary TextField was added for validation, remove or convert it into a proper example after verification

### Documentation
- [ ] Update USF.md
  - [ ] Add "Offloading Heavy Work" section
  - [ ] Add examples of offload usage
  - [ ] Add examples of advanced coroutine usage

- [ ] Update USF-QUICKSTART.md
  - [ ] Update async increment example with offload
  - [ ] Update documentation comments

- [ ] Update AGENTS.md
  - [ ] Update USF section with main-thread-first approach
  - [ ] Add note about offload helper

### Follow-up
- [ ] Add lint rule to detect blocking operations
  - [ ] Create custom lint detector
  - [ ] Test lint rule catches common issues
  - [ ] Document lint rule

- [ ] Audit existing ViewModels
  - [ ] Search for blocking operations in process() methods
  - [ ] Fix any issues found
  - [ ] Document patterns

- [ ] Create migration guide
  - [ ] Document how to migrate existing ViewModels
  - [ ] Add examples of common patterns
  - [ ] Add troubleshooting section
