package sh.kau.playground.usf

sealed class TestInput {
  data object TestInput1 : TestInput()

  data object TestInput2 : TestInput()

  data object TestInput3 : TestInput()

  data class TestNumberInput(val value: Int) : TestInput()

  data class TestErrorThrowInput(val error: Throwable = RuntimeException("error directly thrown")) :
      TestInput()

  data class TestErrorFlowInput(val error: Throwable = RuntimeException("error from within flow")) :
      TestInput()

  data class TestDelayedInput(val delayMs: Long) : TestInput()

  data object TestNullableEffectInput : TestInput()

  data class TestErrorInOutputToUiStateInput(
      val error: Throwable = RuntimeException("error from within flow")
  ) : TestInput()

  data object TestErrorInOutputToEffectsInput : TestInput()

  data object TestErrorInOutputToEffectsFlow : TestInput()
}

sealed class TestOutput {
  data object TestOutput1 : TestOutput()

  data object TestOutput2 : TestOutput()

  data object TestOutput3 : TestOutput()

  data class TestNumberOutput(val value: Int) : TestOutput()

  data class TestErrorThrowOutput(val error: Throwable) : TestOutput()

  data class TestErrorFlowOutput(val error: Throwable) : TestOutput()

  data class TestDelayedOutput(val delayMs: Long) : TestOutput()

  data object TestNullableEffectOutput : TestOutput()

  data object TestErrorInOutputToUiStateOutput : TestOutput()

  data object TestErrorInOutputToEffectsOutput : TestOutput()

  data object TestErrorInOutputToEffectsFlowOutput : TestOutput()
}

sealed class TestEffect {
  data object TestEffect1 : TestEffect()

  data object TestEffect2 : TestEffect()

  data class TestNumberEffect(val value: Int) : TestEffect()

  data class TestDelayedEffect(val delayMs: Long) : TestEffect()
}

data class TestUiState(
    val text: String,
    val number: Int = -1,
)
