package sh.kau.playground.usf.impl

import kotlin.text.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import sh.kau.playground.usf.TestEffect
import sh.kau.playground.usf.TestEvent
import sh.kau.playground.usf.TestState
import sh.kau.playground.usf.scope.ResultScope

@OptIn(ExperimentalCoroutinesApi::class)
class UsfViewModelTest {

  @JvmField @RegisterExtension val coroutineTestRule = CoroutineTestRule()

  private fun TestScope.createTestViewModel(
      customInitialStateProvider: (() -> TestState)? = null,
  ): TestViewModel {
    return TestViewModel(
        customInitialStateProvider = customInitialStateProvider,
        coroutineScope = backgroundScope,
        processingDispatcher = coroutineTestRule.testDispatcher,
    )
  }

  @Test
  @DisplayName("Initial view state is emitted upon subscription")
  fun initialViewStateEmittedOnSubscription() = runTest {
    val viewModel = createTestViewModel()

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    assertThat(viewStates).hasSize(1)
    assertThat(viewStates[0]).isEqualTo(TestState())
    assertThat(effects).isEmpty()

    jobVs.cancel()
    jobEf.cancel()
  }

  @Test
  @DisplayName("Process a simple event leading to state change and effect emission")
  fun input_StateChangeAndEffect() = runTest {
    val viewModel = createTestViewModel()

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial state

    viewModel.input(TestEvent.ComplexEvent(value = 5, label = "TestEffect"))
    runCurrent()

    assertThat(viewStates).hasSize(2) // Initial + Updated
    assertThat(viewStates.last()).isEqualTo(TestState(name = "Initial State", counter = 5))

    assertThat(effects).hasSize(1)
    assertThat(effects.last()).isEqualTo(TestEffect.NamedEffect("TestEffect"))
  }

  @Test
  @DisplayName("Process multiple events in sequence")
  fun processMultipleEvents_InSequence() = runTest {
    val viewModel = createTestViewModel()

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial state

    viewModel.input(TestEvent.IncrementCounter)
    runCurrent()

    viewModel.input(TestEvent.UpdateName("New Name"))
    runCurrent()

    viewModel.input(TestEvent.EmitEffect)
    runCurrent()

    assertThat(viewStates.last()).isEqualTo(TestState(name = "New Name", counter = 1))
    assertThat(effects).containsExactly(TestEffect.SimpleEffect)
  }

  @Test
  @DisplayName("View state reflects final state after a batch, effects are preserved")
  fun viewStateConflation_EffectsPreserved() = runTest {
    val viewModel = createTestViewModel()
    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Ensure subscriptions are active and initial state is collected

    viewModel.input(TestEvent.IncrementCounter) // counter = 1
    viewModel.input(TestEvent.EmitEffect) // effect 1
    viewModel.input(TestEvent.UpdateName("Intermediate Name")) // name = Intermediate, counter = 1
    viewModel.input(TestEvent.IncrementCounter) // name = Intermediate, counter = 2
    viewModel.input(TestEvent.EmitEffect) // effect 2
    viewModel.input(TestEvent.UpdateName("Final Name")) // name = Final, counter = 2
    runCurrent() // Process all events in the batch and collect updates

    assertThat(viewStates.first()).isEqualTo(TestState())
    assertThat(viewStates.last()).isEqualTo(TestState(name = "Final Name", counter = 2))
    assertThat(effects).hasSize(2)
    assertThat(effects).containsExactly(TestEffect.SimpleEffect, TestEffect.SimpleEffect)

    jobVs.cancel()
    jobEf.cancel()
    runCurrent() // Process cancellations

    val viewModel2 = createTestViewModel()
    val viewStates2 = mutableListOf<TestState>()
    val effects2 = mutableListOf<TestEffect>()

    val jobVs2 = backgroundScope.launch { viewModel2.state.toList(viewStates2) }
    val jobEf2 = backgroundScope.launch { viewModel2.effects.toList(effects2) }
    runCurrent() // Initial collections

    viewModel2.input(TestEvent.IncrementCounter) // counter = 1, state update
    viewModel2.input(TestEvent.EmitEffect) // effect 1
    runCurrent() // Process these two

    viewModel2.input(TestEvent.UpdateName("Intermediate Name")) // name = Intermediate, state update
    viewModel2.input(TestEvent.IncrementCounter) // counter = 2, state update
    viewModel2.input(TestEvent.EmitEffect) // effect 2
    viewModel2.input(TestEvent.UpdateName("Final Name")) // name = Final, state update
    runCurrent() // Process these

    assertThat(viewStates2.last()).isEqualTo(TestState(name = "Final Name", counter = 2))
    assertThat(effects2).hasSize(2)
    assertThat(effects2).containsExactly(TestEffect.SimpleEffect, TestEffect.SimpleEffect)

    jobVs2.cancel()
    jobEf2.cancel()
  }

  @Test
  @DisplayName("Duplicate state updates are de-duped by state, effects are not")
  fun duplicateStateDeDuped_EffectsNot() = runTest {
    val viewModel = createTestViewModel()

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial state

    viewModel.input(TestEvent.UpdateName("Same Name"))
    runCurrent()

    viewModel.input(TestEvent.UpdateName("Same Name")) // Duplicate state update
    runCurrent()

    viewModel.input(TestEvent.EmitEffect)
    runCurrent()
    viewModel.input(TestEvent.EmitEffect) // Duplicate effect
    runCurrent()

    // Check that the state did not emit a new value for the duplicate update
    // If TestState("Initial State") -> TestState("Same Name") is one change.
    // Then TestState("Same Name") -> TestState("Same Name") is no change.
    // Initial state + one update for "Same Name"
    val expectedViewStates = listOf(TestState(), TestState(name = "Same Name"))
    assertThat(viewStates).containsExactlyElementsOf(expectedViewStates)

    assertThat(effects).hasSize(2)
    assertThat(effects).containsExactly(TestEffect.SimpleEffect, TestEffect.SimpleEffect)
  }

  @Test
  @DisplayName("Pipeline terminates after 5s of no subscribers and restarts on new subscription")
  fun pipelineTerminationAndRestart() = runTest {
    val viewModel = createTestViewModel()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse() // Initially inactive

    val vsCollector1 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue() // Active after first subscription

    vsCollector1.cancel()
    runCurrent()
    assertThat(viewModel.subscriberCount.get()).isEqualTo(0)

    advanceTimeBy(4.seconds)
    assertThat(viewModel.mainJob?.isActive).isTrue() // Still active before 5s timeout

    advanceTimeBy(1.seconds + 100.milliseconds) // Exceed 5s timeout (default for
    // CaperConfigKey.VIEW_MODEL_MAX_TIMEOUT_SECONDS)
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse() // Terminated after timeout

    // New subscription should restart the pipeline
    val vsCollector2 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue() // Active again

    vsCollector2.cancel()
  }

  @Test
  @DisplayName("Error in processEvent is caught and VM continues processing subsequent events")
  fun errorInInput_VmContinues() = runTest {
    val viewModel = createTestViewModel().apply { throwErrorInProcessEvent = true }

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial state

    // Event 1 (Success)
    viewModel.input(TestEvent.IncrementCounter)
    runCurrent()

    // Event 2 (Causes error in TestViewModel.processEvent)
    viewModel.input(TestEvent.EventThatCausesError)
    runCurrent()

    // Event 3 (Success, should still be processed)
    viewModel.input(TestEvent.UpdateName("After Error"))
    runCurrent()

    assertThat(viewStates.last()).isEqualTo(TestState(name = "After Error", counter = 1))
    assertThat(effects).isEmpty() // No effects emitted by these specific events in TestViewModel

    // Verify error was logged (optional, depends on logger mock or if BaseViewModel rethrows)
    // BaseViewModel catches and logs. It doesn't rethrow to stop the pipeline.
  }

  @Test
  @DisplayName("New subscriber receives the last emitted state, not initial state")
  fun newSubscriberReceivesLastState() = runTest {
    val viewModel = createTestViewModel()

    val viewStates1 = mutableListOf<TestState>()
    val job1 = backgroundScope.launch { viewModel.state.toList(viewStates1) }
    runCurrent()

    viewModel.input(TestEvent.UpdateName("Updated Name"))
    runCurrent()
    assertThat(viewStates1.last()).isEqualTo(TestState(name = "Updated Name"))

    job1.cancel()
    runCurrent()

    val viewStates2 = mutableListOf<TestState>()
    val job2 = backgroundScope.launch { viewModel.state.toList(viewStates2) }
    runCurrent()

    assertThat(viewStates2).hasSize(1)
    assertThat(viewStates2.first()).isEqualTo(TestState(name = "Updated Name"))

    job2.cancel()
  }

  @Test
  @DisplayName("Events are processed and state updated even if no subscribers initially")
  fun testEventsProcessedWhenNoSubscribersInitially() = runTest {
    val initialProvidedState = TestState("Initial", 0)
    val viewModel = createTestViewModel(customInitialStateProvider = { initialProvidedState })

    // Process events before any subscribers register
    viewModel.input(TestEvent.IncrementCounter) // counter = 1
    viewModel.input(TestEvent.UpdateName("Updated Name")) // name = "Updated Name"
    runCurrent() // Allow events to be processed by the channel and pipeline (if active or started
    // by send)

    // Now subscribe to state
    val viewStates = mutableListOf<TestState>()
    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    runCurrent() // Collect states

    // The new subscriber gets the initial state from stateIn first,
    // then any distinct intermediate states, and finally the latest processed state.
    assertThat(viewStates.first())
        .isEqualTo(initialProvidedState) // Check the initial state from stateIn
    assertThat(viewStates.last()).isEqualTo(TestState("Updated Name", 1)) // Check the final state

    // Also check effects, though none are emitted by these events
    val effects = mutableListOf<TestEffect>()
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()
    assertThat(effects).isEmpty()

    jobVs.cancel()
    jobEf.cancel()
  }

  @Test
  @DisplayName("Consumed effects are not replayed to new subscribers")
  fun testConsumedEffectsNotReplayedToNewSubscribers() = runTest {
    val viewModel = createTestViewModel()

    // First subscriber collects an effect
    val effects1 = mutableListOf<TestEffect>()
    val jobEf1 = backgroundScope.launch { viewModel.effects.toList(effects1) }
    runCurrent()

    viewModel.input(TestEvent.EmitEffect)
    runCurrent()
    assertThat(effects1).containsExactly(TestEffect.SimpleEffect)
    jobEf1.cancel()
    runCurrent()

    // Second subscriber connects later
    val effects2 = mutableListOf<TestEffect>()
    val jobEf2 = backgroundScope.launch { viewModel.effects.toList(effects2) }
    runCurrent()

    assertThat(effects2).isEmpty() // Should not receive the already consumed effect

    jobEf2.cancel()
  }

  @Test
  @DisplayName(
      "Pipeline stays active as long as there is at least one subscriber (either to state or effects)"
  )
  fun testPipelineStaysActiveWithAnySubscriber() = runTest {
    val viewModel = createTestViewModel()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse() // Initially inactive

    // 1. Subscribe to ViewState - Pipeline becomes active
    val jobVs1 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue()

    // 2. Subscribe to Effects - Pipeline stays active
    val jobEf1 = backgroundScope.launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue()

    // 3. Unsubscribe from ViewState - Pipeline stays active (due to effects subscriber)
    jobVs1.cancel()
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue()

    // 4. Unsubscribe from Effects - Pipeline should schedule termination (and terminate after
    // timeout)
    jobEf1.cancel()
    runCurrent()
    assertThat(viewModel.subscriberCount.get()).isEqualTo(0)
    // advanceTimeBy(BaseUsfViewModel.DEFAULT_STOP_TIMEOUT_MS + 100) // Assuming a known timeout
    advanceTimeBy(5.seconds + 100.milliseconds) // Using the known 5s timeout from previous tests
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()
  }

  @Test
  @DisplayName("Pipeline terminates when all of multiple subscribers are gone after timeout")
  fun testPipelineTerminationWithMultipleSubscribers() = runTest {
    val viewModel = createTestViewModel()

    val jobVs1 = backgroundScope.launch { viewModel.state.collect {} }
    val jobEf1 = backgroundScope.launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive).isTrue()
    assertThat(viewModel.subscriberCount.get()).isEqualTo(2)

    jobVs1.cancel()
    jobEf1.cancel()

    runCurrent() // All subscribers are gone
    assertThat(viewModel.subscriberCount.get()).isEqualTo(0)
    assertThat(viewModel.mainJob?.isActive).isTrue() // Still active Still active until timeout

    advanceTimeBy(5.seconds + 100.milliseconds) // Exceed timeout
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse() // Terminated
  }

  @Test
  @DisplayName("Async operation updates state after delay")
  fun asyncStateUpdate_CompletesAndUpdatesState() = runTest {
    val viewModel = createTestViewModel()
    val viewStates = mutableListOf<TestState>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    runCurrent() // Initial state

    val asyncDelay = 100L
    viewModel.input(TestEvent.AsyncOperationEvent(delayMillis = asyncDelay, targetCounter = 123))
    runCurrent() // Process the event, launch async job

    assertThat(viewStates.last().counter).isEqualTo(0) // State not yet updated

    advanceTimeBy(asyncDelay - 10)
    runCurrent()
    assertThat(viewStates.last().counter).isEqualTo(0) // Still not updated

    advanceTimeBy(10 + 1) // Advance past the delay
    runCurrent() // Allow the async operation to complete and update state

    assertThat(viewStates.last().counter).isEqualTo(123)
  }

  @Test
  @DisplayName("Async operation emits effect after delay")
  fun asyncEffectEmission_CompletesAndEmitsEffect() = runTest {
    val viewModel = createTestViewModel()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    val asyncDelay = 150L
    val expectedEffect = TestEffect.NamedEffect("AsyncEffect")
    viewModel.input(
        TestEvent.AsyncOperationEvent(
            delayMillis = asyncDelay,
            targetCounter = 0, // Counter doesn't matter here
            effectToEmit = expectedEffect,
        )
    )
    runCurrent() // Process the event, launch async job

    assertThat(effects).isEmpty() // Effect not yet emitted

    advanceTimeBy(asyncDelay + 10) // Advance past the delay
    runCurrent() // Allow the async operation to complete and emit effect

    assertThat(effects).containsExactly(expectedEffect)
  }

  @Test
  @DisplayName("Async operation updates state and emits effect after delay")
  fun asyncStateAndEffect_CompletesAndUpdatesBoth() = runTest {
    val viewModel = createTestViewModel()
    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial collections

    val asyncDelay = 200L
    val expectedEffect = TestEffect.NamedEffect("DualAsync")
    val targetCounterValue = 789
    val targetNameValue = "AsyncName"

    viewModel.input(
        TestEvent.AsyncOperationEvent(
            delayMillis = asyncDelay,
            targetCounter = targetCounterValue,
            newName = targetNameValue,
            effectToEmit = expectedEffect,
        )
    )
    runCurrent() // Process event, launch async

    assertThat(viewStates.last()).isEqualTo(TestState()) // Initial state
    assertThat(effects).isEmpty()

    advanceTimeBy(asyncDelay + 20)
    runCurrent()

    assertThat(viewStates.last())
        .isEqualTo(TestState(name = targetNameValue, counter = targetCounterValue))
    assertThat(effects).containsExactly(expectedEffect)
  }

  @Test
  @DisplayName("Multiple async operations complete in order of their delays")
  fun multipleAsyncOperations_CompleteInDelayOrder() = runTest {
    val viewModel = createTestViewModel()
    val effects = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    val effect1 = TestEffect.NamedEffect("EffectFast")
    val effect2 = TestEffect.NamedEffect("EffectSlow")

    // Launch slow one first, then fast one
    viewModel.input(
        TestEvent.AsyncOperationEvent(delayMillis = 200L, targetCounter = 2, effectToEmit = effect2)
    )
    runCurrent()
    viewModel.input(
        TestEvent.AsyncOperationEvent(delayMillis = 50L, targetCounter = 1, effectToEmit = effect1)
    )
    runCurrent()

    assertThat(effects).isEmpty()

    advanceTimeBy(55L) // Enough for the first (fast) effect
    runCurrent()
    assertThat(effects).containsExactly(effect1)

    advanceTimeBy(200L) // Enough for the second (slow) effect to also complete
    runCurrent()
    assertThat(effects).containsExactly(effect1, effect2)
  }

  @Test
  @DisplayName("Error in async operation does not prevent future event processing")
  fun asyncError_DoesNotPreventFutureEventProcessing() = runTest {
    val viewModel = createTestViewModel()
    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent() // Initial collections

    // 1. Process a normal event first
    viewModel.input(TestEvent.IncrementCounter) // counter = 1
    runCurrent()
    assertThat(viewStates.last().counter).isEqualTo(1)

    // 2. Process an async event that will throw an error
    val asyncErrorDelay = 50L
    viewModel.input(
        TestEvent.AsyncOperationEvent(
            delayMillis = asyncErrorDelay,
            targetCounter = 999, // This state update should not happen
            shouldThrowError = true,
        )
    )
    runCurrent() // Process the event, launch the failing async job

    // Advance time to trigger the error in the async job
    advanceTimeBy(asyncErrorDelay + 10)
    runCurrent() // Allow error to be processed by handler

    // Verify that the error didn't change the state unexpectedly
    assertThat(viewStates.last().counter).isEqualTo(1) // Should still be 1 from the first event

    // 3. Process another normal event
    viewModel.input(TestEvent.UpdateName("After Async Error"))
    runCurrent()

    // Verify the third event was processed correctly
    assertThat(viewStates.last()).isEqualTo(TestState(name = "After Async Error", counter = 1))
    assertThat(effects).isEmpty() // No effects emitted by these specific events

    // Optionally, we could try to assert that the logger in BaseUsfViewModel.handler was called.
    // This would require injecting a mock logger or having access to its output.
  }

  @Test
  @DisplayName("onSubscribed is called when first subscriber connects")
  fun onSubscribedCalledOnFirstSubscriber() = runTest {
    var onSubscribedCallCount = 0
    var onSubscribedStateAtCall: TestState? = null

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            onSubscribedCallCount++
            onSubscribedStateAtCall = state.value
            // Test that we can update state and emit effects in onSubscribed
            updateState { it.copy(name = "Subscribed") }
            emitEffect(TestEffect.NamedEffect("OnSubscribed"))
          }
        }

    assertThat(onSubscribedCallCount).isEqualTo(0) // Not called yet

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    assertThat(onSubscribedCallCount).isEqualTo(1) // Called once when first subscriber connects
    assertThat(onSubscribedStateAtCall).isEqualTo(TestState()) // Initial state was available
    assertThat(viewStates.last()).isEqualTo(TestState(name = "Subscribed")) // State was updated
    assertThat(effects)
        .containsExactly(TestEffect.NamedEffect("OnSubscribed")) // Effect was emitted

    jobVs.cancel()
    jobEf.cancel()
  }

  @Test
  @DisplayName("onSubscribed is called again when pipeline restarts after timeout")
  fun onSubscribedCalledOnPipelineRestart() = runTest {
    var onSubscribedCallCount = 0

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            onSubscribedCallCount++
            updateState { it.copy(counter = it.counter + 1) }
          }
        }

    // First subscription
    val job1 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(onSubscribedCallCount).isEqualTo(1)

    job1.cancel()
    runCurrent()

    // Wait for pipeline to terminate
    advanceTimeBy(5.seconds + 100.milliseconds)
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()

    // Second subscription should restart pipeline and call onSubscribed again
    val job2 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(onSubscribedCallCount).isEqualTo(2) // Called again on restart

    job2.cancel()
  }

  @Test
  @DisplayName("onSubscribed is not called for additional subscribers while pipeline is active")
  fun onSubscribedNotCalledForAdditionalSubscribers() = runTest {
    var onSubscribedCallCount = 0

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            onSubscribedCallCount++
          }
        }

    // First subscriber
    val job1 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(onSubscribedCallCount).isEqualTo(1)

    // Second subscriber while pipeline is still active
    val job2 = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()
    assertThat(onSubscribedCallCount).isEqualTo(1) // Still only called once

    // Third subscriber (effects)
    val job3 = backgroundScope.launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(onSubscribedCallCount).isEqualTo(1) // Still only called once

    job1.cancel()
    job2.cancel()
    job3.cancel()
  }

  @Test
  @DisplayName("onSubscribed can perform async operations")
  fun onSubscribedAsyncOperations() = runTest {
    var asyncOperationCompleted = false

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            coroutineScope.launch {
              delay(50)
              updateState { it.copy(name = "Async Updated") }
              emitEffect(TestEffect.NamedEffect("Async Effect"))
              asyncOperationCompleted = true
            }
          }
        }

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Initially, async operation hasn't completed
    assertThat(asyncOperationCompleted).isFalse()
    assertThat(viewStates.last()).isEqualTo(TestState()) // Initial state
    assertThat(effects).isEmpty()

    // Advance time to complete async operation
    advanceTimeBy(60)
    runCurrent()

    assertThat(asyncOperationCompleted).isTrue()
    assertThat(viewStates.last()).isEqualTo(TestState(name = "Async Updated"))
    assertThat(effects).containsExactly(TestEffect.NamedEffect("Async Effect"))

    jobVs.cancel()
    jobEf.cancel()
  }

  @Test
  @DisplayName("onSubscribed error handling does not break pipeline")
  fun onSubscribedErrorHandling() = runTest {
    var onSubscribedCalled = false

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            onSubscribedCalled = true
            updateState { it.copy(name = "Before Error") }
            error("Test error in onSubscribed")
          }
        }

    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()
    val jobVs = backgroundScope.launch { viewModel.state.toList(viewStates) }
    val jobEf = backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    assertThat(onSubscribedCalled).isTrue()
    // State should still be updated before the error
    assertThat(viewStates.last()).isEqualTo(TestState(name = "Before Error"))

    // Pipeline should still be functional after error in onSubscribed
    viewModel.input(TestEvent.UpdateName("After Error"))
    runCurrent()

    assertThat(viewStates.last()).isEqualTo(TestState(name = "After Error"))

    jobVs.cancel()
    jobEf.cancel()
  }

  @Test
  @DisplayName("Jobs started in onSubscribed are canceled when pipeline terminates")
  fun onSubscribedJobsCanceledOnPipelineTermination() = runTest {
    var jobStarted = false
    var jobCompleted = false
    var jobCanceled = false
    var pipelineJob: Job? = null

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = backgroundScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            // Launch job on coroutineScope which now automatically provides the pipeline scope
            // when the pipeline is active, so jobs are tied to the pipeline lifecycle
            pipelineJob =
                coroutineScope.launch {
                  try {
                    jobStarted = true
                    delay(10.seconds) // Long delay to ensure it gets canceled
                    jobCompleted = true
                  } catch (e: CancellationException) {
                    jobCanceled = true
                    throw e // Re-throw to properly handle cancellation
                  }
                }
          }
        }

    // Start subscription to trigger onSubscribed
    val job = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()

    assertThat(jobStarted).isTrue() // Job should have started
    assertThat(jobCompleted).isFalse() // Job shouldn't have completed yet
    assertThat(jobCanceled).isFalse() // Job shouldn't be canceled yet
    assertThat(pipelineJob?.isActive).isTrue() // Pipeline job should be active

    // Cancel subscription to trigger pipeline termination
    job.cancel()
    runCurrent()

    // Wait for pipeline termination timeout
    advanceTimeBy(5.seconds + 100.milliseconds)
    runCurrent()

    // Verify the pipeline terminated and the job was canceled since coroutineScope
    // now provides the pipeline scope when active
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()
    assertThat(pipelineJob?.isActive ?: false).isFalse() // Should be canceled now!
    assertThat(jobCanceled).isTrue() // Should be canceled
    assertThat(jobCompleted).isFalse() // Should not have completed normally
  }

  @Test
  @DisplayName("Async operations launched during event processing are tied to pipeline scope")
  fun asyncOperationTiedToPipelineScope() = runTest {
    val viewModel = createTestViewModel()
    val viewStates = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    val job = backgroundScope.launch { viewModel.state.toList(viewStates) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    val asyncDelay = 300L
    viewModel.input(
        TestEvent.AsyncOperationEvent(
            delayMillis = asyncDelay,
            targetCounter = 999,
            effectToEmit = TestEffect.SimpleEffect,
        )
    )
    runCurrent() // Process event, launch async job

    // Async operations launched during event processing are now properly scoped to the pipeline
    // and should complete normally while the pipeline is active
    advanceTimeBy(asyncDelay + 10) // Advance beyond the delay
    runCurrent()

    // The async operation should have completed successfully since it's tied to the pipeline scope
    assertThat(viewStates.last().counter).isEqualTo(999) // State should have updated
    assertThat(effects).containsExactly(TestEffect.SimpleEffect) // Effect should have been emitted

    job.cancel()
  }

  @Test
  @DisplayName("Pipeline scope is canceled when view model scope is canceled")
  fun `pipeline scope canceled when view model scope canceled`() = runTest {
    // Create a cancellable scope for the view model
    val viewModelJob = Job()
    val viewModelScope = CoroutineScope(coroutineContext + viewModelJob)

    var pipelineJobStarted = false
    var pipelineJobCompleted = false
    var pipelineJobCanceled = false
    var pipelineJob: Job? = null

    val viewModel =
        object :
            TestViewModel(
                coroutineScope = viewModelScope,
                processingDispatcher = coroutineTestRule.testDispatcher,
            ) {
          override fun ResultScope<TestState, TestEffect>.onSubscribed() {
            // Launch a long-running job in the pipeline scope
            pipelineJob =
                coroutineScope.launch {
                  try {
                    pipelineJobStarted = true
                    delay(10.seconds) // Long delay to ensure it gets canceled
                    pipelineJobCompleted = true
                  } catch (e: CancellationException) {
                    pipelineJobCanceled = true
                    throw e
                  }
                }
          }
        }

    // Start subscription to activate pipeline and trigger onSubscribed
    val stateCollectorJob = backgroundScope.launch { viewModel.state.collect {} }
    runCurrent()

    // Verify pipeline is active and job started
    assertThat(viewModel.mainJob?.isActive).isTrue()
    assertThat(pipelineJobStarted).isTrue()
    assertThat(pipelineJobCompleted).isFalse()
    assertThat(pipelineJobCanceled).isFalse()
    assertThat(pipelineJob?.isActive).isTrue()

    // Cancel the view model scope directly
    viewModelJob.cancel()
    runCurrent()

    // Verify that canceling the view model scope also cancels the pipeline scope
    // and any jobs launched within it
    assertThat(viewModelJob.isCancelled).isTrue()
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse() // Main job should be canceled
    assertThat(pipelineJob?.isActive ?: false).isFalse() // Pipeline job should be canceled
    assertThat(pipelineJobCanceled).isTrue() // Job should have been canceled, not completed
    assertThat(pipelineJobCompleted).isFalse() // Job should not have completed normally

    stateCollectorJob.cancel()
  }
}
