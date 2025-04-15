package sh.kau.playground.usf

sealed class TestEvent {
  data object TestEvent1 : TestEvent()

  data object TestEvent2 : TestEvent()

  data object TestEvent3 : TestEvent()

  data class TestNumberEvent(val value: Int) : TestEvent()

  data class TestErrorThrowEvent(val error: Throwable = RuntimeException("error directly thrown")) :
      TestEvent()

  data class TestErrorFlowEvent(val error: Throwable = RuntimeException("error from within flow")) :
      TestEvent()

  data class TestDelayedEvent(val delayMs: Long) : TestEvent()

  data object TestNullableEffectEvent : TestEvent()

  data class TestErrorInResultToViewStateEvent(
      val error: Throwable = RuntimeException("error from within flow")
  ) : TestEvent()

  data object TestErrorInResultToEffectsEvent : TestEvent()

  data object TestErrorInResultToEffectsFlow : TestEvent()
}

sealed class TestResult {
  data object TestResult1 : TestResult()

  data object TestResult2 : TestResult()

  data object TestResult3 : TestResult()

  data class TestNumberResult(val value: Int) : TestResult()

  data class TestErrorThrowResult(val error: Throwable) : TestResult()

  data class TestErrorFlowResult(val error: Throwable) : TestResult()

  data class TestDelayedResult(val delayMs: Long) : TestResult()

  data object TestNullableEffectResult : TestResult()

  data object TestErrorInResultToViewStateResult : TestResult()

  data object TestErrorInResultToEffectsResult : TestResult()

  data object TestErrorInResultToEffectsFlowResult : TestResult()
}

sealed class TestEffect {
  data object TestEffect1 : TestEffect()

  data object TestEffect2 : TestEffect()

  data class TestNumberEffect(val value: Int) : TestEffect()

  data class TestDelayedEffect(val delayMs: Long) : TestEffect()
}

data class TestViewState(
    val text: String,
    val number: Int = -1,
)
