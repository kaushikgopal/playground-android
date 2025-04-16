package sh.kau.playground.usf.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import sh.kau.playground.usf.TestEffect
import sh.kau.playground.usf.TestEffect.TestDelayedEffect
import sh.kau.playground.usf.TestInput
import sh.kau.playground.usf.TestInput.*
import sh.kau.playground.usf.TestOutput
import sh.kau.playground.usf.TestOutput.*
import sh.kau.playground.usf.TestUiState
import sh.kau.playground.usf.TestUsfLogger
import sh.kau.playground.usf.UsfImpl

class TestViewModel(coroutineScope: CoroutineScope, initFlow: Flow<Int> = emptyFlow()) :
    UsfImpl<TestInput, TestOutput, TestUiState, TestEffect, Unit>(
        initialUiState = TestUiState("[US] initial"),
        coroutineScope = coroutineScope,
        logger = TestUsfLogger,
    ) {

  init {
    initFlow.onEach { processInput(TestNumberInput(it)) }.launchIn(coroutineScope)
  }

  override fun inputToOutputFlow(input: TestInput): Flow<TestOutput> =
      when (input) {
        TestInput1 -> flowOf(TestOutput1)
        TestInput2 -> flowOf(TestOutput2)
        TestInput3 -> flowOf(TestOutput3)
        is TestNumberInput -> flowOf(TestNumberOutput(input.value))
        is TestErrorThrowInput -> throw input.error
        is TestErrorFlowInput -> flow { throw input.error }
        is TestDelayedInput ->
            flowOf(TestDelayedOutput(input.delayMs)).onEach { delay(input.delayMs) }

        TestNullableEffectInput -> flowOf(TestNullableEffectOutput)
        is TestErrorInOutputToUiStateInput -> flow { throw input.error }
        is TestErrorInOutputToEffectsInput -> flowOf(TestErrorInOutputToEffectsOutput)
        is TestErrorInOutputToEffectsFlow -> flowOf(TestErrorInOutputToEffectsFlowOutput)
      }

  override suspend fun outputToUiState(
      currentUiState: TestUiState,
      output: TestOutput
  ): TestUiState =
      when (output) {
        TestOutput1 -> currentUiState.copy(text = "[US] 1 ")
        TestOutput2 -> currentUiState.copy(text = "[US] 2 ")
        TestOutput3 -> currentUiState.copy(text = "[US] 3 ")
        is TestNumberOutput -> currentUiState.copy(number = output.value)
        is TestErrorThrowOutput -> currentUiState
        is TestErrorFlowOutput -> currentUiState
        is TestDelayedOutput -> currentUiState.copy(text = "[US] delayed ${output.delayMs}")
        TestNullableEffectOutput -> currentUiState.copy(text = "[US] nullable effect")
        TestErrorInOutputToUiStateOutput -> throw Exception("Error in resultToUiState")
        TestErrorInOutputToEffectsOutput -> currentUiState
        TestErrorInOutputToEffectsFlowOutput -> currentUiState
      }

  override fun outputToEffects(output: TestOutput): Flow<TestEffect> =
      when (output) {
        TestOutput1 -> flowOf(TestEffect.TestEffect1)
        TestOutput2 -> flowOf(TestEffect.TestEffect2)
        TestOutput3 -> emptyFlow()
        is TestNumberOutput -> flowOf(TestEffect.TestNumberEffect(output.value))
        is TestErrorThrowOutput -> emptyFlow()
        is TestErrorFlowOutput -> emptyFlow()
        is TestDelayedOutput -> flowOf(TestDelayedEffect(output.delayMs))
        TestErrorInOutputToEffectsOutput -> flow { throw Exception("Error in resultToEffects") }
        TestNullableEffectOutput -> emptyFlow()
        TestErrorInOutputToUiStateOutput -> emptyFlow()
        TestErrorInOutputToEffectsFlowOutput -> flow { throw Exception("Error in resultToEffects") }
      }
}
