package sh.kau.playground.usf.impl

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import sh.kau.playground.usf.TestEffect
import sh.kau.playground.usf.TestInput
import sh.kau.playground.usf.TestInput.TestErrorInOutputToUiStateInput
import sh.kau.playground.usf.TestUiState

@OptIn(ExperimentalCoroutinesApi::class)
class UsfImplTest {
  private fun TestScope.createViewModel(initFlow: Flow<Int> = emptyFlow()) =
      TestViewModel(
          coroutineScope = backgroundScope,
          initFlow = initFlow,
      )

  @Test
  @DisplayName("core test: input -> output -> (ui state + effect)")
  fun testBasicEmission() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent() // actually make the subscription connection

    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    assertThat(es).hasSize(1).containsExactly(TestEffect.TestEffect1)

    assertThat(vs).hasSize(2).containsExactly(TestUiState("[US] initial"), TestUiState("[US] 1 "))
  }

  @Test
  @DisplayName("core test: input sent through init (flow)")
  fun testHotFlow() = runTest {
    val initFlow = MutableSharedFlow<Int>()
    val viewModel = createViewModel(initFlow)

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    initFlow.emit(42)
    runCurrent()

    assertThat(es).hasSize(1).containsExactly(TestEffect.TestNumberEffect(42))

    assertThat(vs).last().isEqualTo(TestUiState("[US] initial", number = 42))
  }

  @Test
  @DisplayName("initial ui state emitted on subscription")
  fun testInitialViewState() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    assertThat(es).isEmpty()
    assertThat(vs).hasSize(1).containsExactly(TestUiState("[US] initial"))
  }

  @Test
  @DisplayName("core test: process multiple inputs in sequence")
  fun testMultipleEvents() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"), TestUiState("[US] 1 "), TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName("process multiple inputs together, ensure correct conflation behavior")
  fun testPrinputConflation() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    // kicking off the subscription (otherwise the processInput will always get conflated)
    runCurrent() // single runCurrent() here could trigger a conflation problem

    viewModel.processInput(TestInput.TestInput1)
    viewModel.processInput(TestInput.TestInput2)
    runCurrent() // runCurrent() after both inputs in one shot (increasing chances of conflation)

    // ui state can be conflated (since we only want the last one)
    assertThat(vs).hasSize(2).containsExactly(TestUiState("[US] initial"), TestUiState("[US] 2 "))

    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)
  }

  @Test
  @DisplayName("ui state de-duped but not effects, when same input processed multiple times")
  fun testDuplicateViewState() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // Send same input twice
    viewModel.processInput(TestInput.TestInput1)
    viewModel.processInput(TestInput.TestInput1)
    viewModel.processInput(TestInput.TestInput1)
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Effects should be emitted for both inputs
    assertThat(es).hasSize(4)
    assertThat(es.distinct()).containsExactly(TestEffect.TestEffect1)

    // ViewState should only emit once for duplicate state
    assertThat(vs)
        .hasSize(2) // Initial state + one update
        .containsExactly(TestUiState("[US] initial"), TestUiState("[US] 1 "))
  }

  @Test
  @DisplayName("core test: multiple inputs that take different amount of time to finish")
  fun testConcurrentEventOrder() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // Send multiple delayed inputs rapidly
    viewModel.processInput(TestInput.TestDelayedInput(100))
    viewModel.processInput(TestInput.TestDelayedInput(50))
    viewModel.processInput(TestInput.TestDelayedInput(25))
    advanceTimeBy(101)

    // Verify that effects are processed in order of completion, not submission
    assertThat(es)
        .hasSize(3)
        .containsExactly(
            TestEffect.TestDelayedEffect(25),
            TestEffect.TestDelayedEffect(50),
            TestEffect.TestDelayedEffect(100),
        )
  }

  @Test
  @DisplayName("inputs sent when subscribers not present, are not lost")
  fun testEventsNotLostWithoutSubscribers() = runTest {
    val viewModel = createViewModel()

    // Send two inputs before adding subscribers
    viewModel.processInput(TestInput.TestInput1)
    viewModel.processInput(TestInput.TestNumberInput(42))
    viewModel.processInput(TestInput.TestInput3)
    runCurrent()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()

    // Add subscribers after input was processed
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    assertThat(vs.last()).isEqualTo(TestUiState("[US] 3 ", number = 42))

    assertThat(es)
        .hasSize(2)
        .containsExactly(TestEffect.TestEffect1, TestEffect.TestNumberEffect(42))
  }

  @Test
  @DisplayName("inputs sent when subscribers disconnect, and then reconnect, are never lost")
  fun testEffectNotLostWithoutSubscribers() = runTest {
    val viewModel = createViewModel()

    // Start collecting effects and uiState with first subscriber
    val es1 = mutableListOf<TestEffect>()
    val vs1 = mutableListOf<TestUiState>()
    val jobEs1 = backgroundScope.launch { viewModel.effects.toList(es1) }
    val jobVs1 = backgroundScope.launch { viewModel.uiState.toList(vs1) }
    runCurrent()

    // Process inputs with first subscriber connected
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()
    assertThat(es1).hasSize(1)
    assertThat(vs1).hasSize(2).containsExactly(TestUiState("[US] initial"), TestUiState("[US] 1 "))

    // Cancel subscriptions
    jobEs1.cancel()
    jobVs1.cancel()
    runCurrent()

    // Process inputs while no subscribers are connected
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Start collecting effects and uiState with second subscriber
    val es2 = mutableListOf<TestEffect>()
    val vs2 = mutableListOf<TestUiState>()
    backgroundScope.launch { viewModel.effects.toList(es2) }
    backgroundScope.launch { viewModel.uiState.toList(vs2) }
    runCurrent()

    // Second subscription should see latest state and replayed effect
    assertThat(vs2).last().isEqualTo(TestUiState("[US] 2 "))

    // Verify that second subscriber received the effects emitted during disconnection
    assertThat(es2).hasSize(1).containsExactly(TestEffect.TestEffect2)
  }

  @Test
  @DisplayName("5s timeout for ui state, always emits the latest known value")
  fun testViewStateAlwaysLatest() = runTest {
    val viewModel = createViewModel()

    // First subscriber
    val vs = mutableListOf<TestUiState>()
    val job1A = backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // Process regular input
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Cancel first subscription
    job1A.cancel()
    vs.clear()
    runCurrent()

    // Advance time less than timeout of 5 seconds
    advanceTimeBy(4_000)

    // New subscription
    val job2A = backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()
    // should receive the latest ui state
    assertThat(vs.last()).isEqualTo(TestUiState("[US] 1 "))

    // Cancel second subscription
    job2A.cancel()
    vs.clear()
    runCurrent()

    // Advance time more than timeout of 5 seconds
    advanceTimeBy(6_000)

    // New subscription(s)
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // should still see latest ui state
    assertThat(vs).last().isEqualTo(TestUiState("[US] 1 "))
  }

  @Test
  @DisplayName(
      "5s timeout for effects, emits to first new subscriber only, if no subscribers connected")
  fun testViewEffectsConsumedOnlyOnce() = runTest {
    val viewModel = createViewModel()

    // Process regular input that emits an effect
    // but no subscribers
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // First subscriber joins
    val es = mutableListOf<TestEffect>()
    val job1B = backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()

    // Effect consumed by first subscriber
    assertThat(es).hasSize(1).containsExactly(TestEffect.TestEffect1)

    // Cancel first subscription
    job1B.cancel()
    es.clear()
    runCurrent()

    // Advance time less than timeout of 5 seconds
    advanceTimeBy(4_000)

    // New subscription
    val job2B = backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()
    // should not receive any effect
    assertThat(es).isEmpty()

    // Cancel second subscription
    job2B.cancel()
    es.clear()
    runCurrent()

    // Advance time more than timeout of 5 seconds
    advanceTimeBy(6_000)

    // New subscription(s)
    backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()

    // should not receive any effect
    assertThat(es).isEmpty()
  }

  @Test
  @DisplayName("5s timeout for effects, no influence if subscriber was connected and consumed")
  fun testViewEffectsOn5sTimeout() = runTest {
    val viewModel = createViewModel()

    // First subscriber
    val es = mutableListOf<TestEffect>()
    val job1B = backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()

    // Process regular input
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()
    // Effect consumed by first subscriber
    assertThat(es).hasSize(1).containsExactly(TestEffect.TestEffect1)

    // Cancel first subscription
    job1B.cancel()
    es.clear()
    runCurrent()

    // Advance time less than timeout of 5 seconds
    advanceTimeBy(4_000)

    // New subscription
    val job2B = backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()
    // should not receive any effect
    assertThat(es).isEmpty()

    // Cancel second subscription
    job2B.cancel()
    es.clear()
    runCurrent()

    // Advance time more than timeout of 5 seconds
    advanceTimeBy(6_000)

    // New subscription(s)
    backgroundScope.launch { viewModel.effects.toList(es) }
    runCurrent()

    // should not receive any effect
    assertThat(es).isEmpty()
  }

  @Test
  @DisplayName("error within input to output flow, flow continues processing inputs after")
  fun testErrorHandlingForFlowErrors() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // First send an input that will succeed
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Then send an input that will cause an error
    viewModel.processInput(TestInput.TestErrorFlowInput())
    runCurrent()

    // Finally send another valid input
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify that effects and ui states from valid inputs were processed
    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"), TestUiState("[US] 1 "), TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName("direct error thrown in inputToOutputFlow, flow continues processing inputs after")
  fun testErrorHandlingForDirectErrors() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // First send an input that will succeed
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Then send an input that will cause an error
    viewModel.processInput(TestInput.TestErrorThrowInput())
    runCurrent()

    // Finally send another valid input
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify that effects and ui states from valid inputs were processed
    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"), TestUiState("[US] 1 "), TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName("error in outputToViewState, flow continues processing subsequent inputs")
  fun testErrorInOutputToUiState() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // First send an input that will succeed
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Then send an input that will cause an error in outputToViewState
    viewModel.processInput(TestErrorInOutputToUiStateInput())
    runCurrent()

    // Finally send another valid input
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify that effects from valid inputs were processed
    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    // Verify that ui states from valid inputs were processed, and the error did not stop the flow
    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"), TestUiState("[US] 1 "), TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName("error in OutputToEffects, flow continues processing subsequent inputs")
  fun testErrorInOutputToEffects() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // First send an input that will succeed
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Then send an input that will cause an error in OutputToEffects
    viewModel.processInput(TestInput.TestErrorInOutputToEffectsInput)
    runCurrent()

    // Finally send another valid input
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify that effects from valid inputs were processed
    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    // Verify that ui states from all inputs were processed, including the one with error in
    // effects
    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"),
            TestUiState("[US] 1 "),
            // TestViewState("[US] 1 "), // distinctUntilChanged will filter this out
            TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName(
      "when error occurs in OutputToEffects flow, flow continues processing subsequent inputs")
  fun testErrorInOutputToEffectsFlow() = runTest {
    val viewModel = createViewModel()

    val vs = mutableListOf<TestUiState>()
    val es = mutableListOf<TestEffect>()
    backgroundScope.launch { viewModel.effects.toList(es) }
    backgroundScope.launch { viewModel.uiState.toList(vs) }
    runCurrent()

    // First send an input that will succeed
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Then send an input that will cause an error in OutputToEffects
    viewModel.processInput(TestInput.TestErrorInOutputToEffectsFlow)
    runCurrent()

    // Finally send another valid input
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify that effects from valid inputs were processed
    assertThat(es).hasSize(2).containsExactly(TestEffect.TestEffect1, TestEffect.TestEffect2)

    // Verify that ui states from all inputs were processed, including the one with error in
    // effects
    assertThat(vs)
        .hasSize(3)
        .containsExactly(
            TestUiState("[US] initial"),
            TestUiState("[US] 1 "),
            // TestViewState("[US] 1 "), // distinctUntilChanged will filter this out
            TestUiState("[US] 2 "))
  }

  @Test
  @DisplayName("when all subscribers are gone for 5s, jobs terminate")
  fun testJobsTerminateAfterTimeout() = runTest {
    val viewModel = createViewModel()

    // make sure no subscribers
    assertThat(viewModel.subscribers).isEqualTo(0)
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()
    runCurrent()

    // Subscribe to uiState and effects
    val job1 = launch { viewModel.uiState.collect {} }
    val job2 = launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(viewModel.subscribers).isEqualTo(2)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // Unsubscribe all
    job1.cancel()
    runCurrent()
    job2.cancel()
    runCurrent()

    assertThat(viewModel.subscribers).isEqualTo(0)

    // Advance time by just under 5s, check that mainJob is still active
    advanceTimeBy(4.seconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // Advance to 5s, now the job should terminate
    advanceTimeBy(1.seconds + 1.milliseconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()
  }

  @Test
  @DisplayName("both uiState and effects should respect the 5s timeout before terminating")
  fun testTimeoutRespectedForBothViewStateAndEffects() = runTest {
    val viewModel = createViewModel()

    // Subscribe to uiState first
    val job1 = launch { viewModel.uiState.collect {} }
    runCurrent()
    assertThat(viewModel.subscribers).isEqualTo(1)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // Unsubscribe uiState
    job1.cancel()
    advanceTimeBy(4.seconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // subscribe to effects now (1 s left for ui state to timeout)
    val job2 = launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // advance time (uiState should have timed out + effects still active)
    advanceTimeBy(1.seconds + 1.milliseconds)
    job2.cancel()
    runCurrent()

    // should still be active since effects shouldn't have timed out
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // everything should be timed out now
    advanceTimeBy(4.seconds + 1.milliseconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()
  }

  @Test
  @DisplayName("VM cleans up after itself, even with multiple subscriptions")
  fun testCleanupWithMultipleSubscriptions() = runTest {
    val viewModel = createViewModel()

    // Subscribe to uiState first
    val job1 = launch { viewModel.uiState.collect {} }
    val job2 = launch { viewModel.uiState.collect {} }
    val job3 = launch { viewModel.effects.collect {} }
    runCurrent()
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    // Unsubscribe uiState
    job1.cancel()
    job3.cancel()
    advanceTimeBy(5.seconds + 1.milliseconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isTrue()

    job2.cancel()
    advanceTimeBy(5.seconds + 1.milliseconds)
    assertThat(viewModel.mainJob?.isActive ?: false).isFalse()
  }

  @Test
  @DisplayName("second subscriber receives last emitted state, not initial state")
  fun testSecondSubscriberReceivesLastEmittedState() = runTest {
    val viewModel = createViewModel()

    // First subscriber
    val vs1 = mutableListOf<TestUiState>()
    val job1 = backgroundScope.launch { viewModel.uiState.toList(vs1) }
    runCurrent()

    // Process an input to update the ui state
    viewModel.processInput(TestInput.TestInput1)
    runCurrent()

    // Verify first subscriber received initial state and updated state
    assertThat(vs1).hasSize(2).containsExactly(TestUiState("[US] initial"), TestUiState("[US] 1 "))

    // Cancel first subscription
    job1.cancel()
    runCurrent()

    // Second subscriber
    val vs2 = mutableListOf<TestUiState>()
    backgroundScope.launch { viewModel.uiState.toList(vs2) }
    runCurrent()

    // Verify second subscriber immediately receives the last emitted state (not the initial state)
    assertThat(vs2)
        .hasSize(1)
        .containsExactly(TestUiState("[US] 1 "))
        .doesNotContain(TestUiState("[US] initial"))

    // Process another input to ensure the flow continues working
    viewModel.processInput(TestInput.TestInput2)
    runCurrent()

    // Verify second subscriber receives the new state
    assertThat(vs2).hasSize(2).containsExactly(TestUiState("[US] 1 "), TestUiState("[US] 2 "))
  }
}
