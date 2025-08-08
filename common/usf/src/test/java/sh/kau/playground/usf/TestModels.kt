package sh.kau.playground.usf

sealed class TestEvent {
  object IncrementCounter : TestEvent()

  data class UpdateName(val name: String) : TestEvent()

  object EmitEffect : TestEvent()

  data class ComplexEvent(val value: Int, val label: String) : TestEvent()

  object EventThatCausesError : TestEvent()

  data class AsyncOperationEvent(
      val delayMillis: Long,
      val targetCounter: Int,
      val newName: String? = null,
      val effectToEmit: TestEffect? = null,
      val shouldThrowError: Boolean = false
  ) : TestEvent()
}

data class TestState(val name: String = "Initial State", val counter: Int = 0)

sealed class TestEffect {
  object SimpleEffect : TestEffect()

  data class NamedEffect(val name: String) : TestEffect()
}
