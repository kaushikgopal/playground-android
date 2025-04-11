package sh.kau.playground.usf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import sh.kau.playground.usf.TestEffect.TestDelayedEffect
import sh.kau.playground.usf.TestEvent.*
import sh.kau.playground.usf.TestResult.*

class TestViewModel(coroutineScope: CoroutineScope, initFlow: Flow<Int> = emptyFlow()) :
    UsfImpl<TestEvent, TestResult, TestViewState, TestEffect, Unit>(
        initialUiState = TestViewState("[VS] initial"),
        coroutineScope = coroutineScope,
        logger = TestUsfLogger,
    ) {

  init {
    initFlow.onEach { processInput(TestNumberEvent(it)) }.launchIn(coroutineScope)
  }

  override fun eventToResultFlow(event: TestEvent): Flow<TestResult> =
      when (event) {
        TestEvent1 -> flowOf(TestResult1)
        TestEvent2 -> flowOf(TestResult2)
        TestEvent3 -> flowOf(TestResult3)
        is TestNumberEvent -> flowOf(TestNumberResult(event.value))
        is TestErrorThrowEvent -> throw event.error
        is TestErrorFlowEvent -> flow { throw event.error }
        is TestDelayedEvent ->
            flowOf(TestDelayedResult(event.delayMs)).onEach { delay(event.delayMs) }

        TestNullableEffectEvent -> flowOf(TestNullableEffectResult)
        is TestErrorInResultToViewStateEvent -> flow { throw event.error }
        is TestErrorInResultToEffectsEvent -> flowOf(TestErrorInResultToEffectsResult)
        is TestErrorInResultToEffectsFlow -> flowOf(TestErrorInResultToEffectsFlowResult)
      }

  override suspend fun resultToViewState(
      currentViewState: TestViewState,
      result: TestResult
  ): TestViewState =
      when (result) {
        TestResult1 -> currentViewState.copy(text = "[VS] 1 ")
        TestResult2 -> currentViewState.copy(text = "[VS] 2 ")
        TestResult3 -> currentViewState.copy(text = "[VS] 3 ")
        is TestNumberResult -> currentViewState.copy(number = result.value)
        is TestErrorThrowResult -> currentViewState
        is TestErrorFlowResult -> currentViewState
        is TestDelayedResult -> currentViewState.copy(text = "[VS] delayed ${result.delayMs}")
        TestNullableEffectResult -> currentViewState.copy(text = "[VS] nullable effect")
        TestErrorInResultToViewStateResult -> throw Exception("Error in resultToViewState")
        TestErrorInResultToEffectsResult -> currentViewState
        TestErrorInResultToEffectsFlowResult -> currentViewState
      }

  override fun resultToEffects(result: TestResult): Flow<TestEffect> =
      when (result) {
        TestResult1 -> flowOf(TestEffect.TestEffect1)
        TestResult2 -> flowOf(TestEffect.TestEffect2)
        TestResult3 -> emptyFlow()
        is TestNumberResult -> flowOf(TestEffect.TestNumberEffect(result.value))
        is TestErrorThrowResult -> emptyFlow()
        is TestErrorFlowResult -> emptyFlow()
        is TestDelayedResult -> flowOf(TestDelayedEffect(result.delayMs))
        TestErrorInResultToEffectsResult -> flow { throw Exception("Error in resultToEffects") }
        TestNullableEffectResult -> emptyFlow()
        TestErrorInResultToViewStateResult -> emptyFlow()
        TestErrorInResultToEffectsFlowResult -> flow { throw Exception("Error in resultToEffects") }
      }
}
