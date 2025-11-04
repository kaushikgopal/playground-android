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
      val shouldThrowError: Boolean = false,
  ) : TestEvent()

  // Events for effect-to-event transformation testing
  object PluginTriggeredReset : TestEvent()

  data class PluginTriggeredCounterUpdate(val newValue: Int) : TestEvent()
}

data class TestState(val name: String = "Initial State", val counter: Int = 0)

sealed class TestEffect {
  object SimpleEffect : TestEffect()

  data class NamedEffect(val name: String) : TestEffect()

  data class CounterUpdated(val value: Int) : TestEffect()
}

// Plugin models for effect-to-event testing
sealed class TestPluginEvent {
  object Increment : TestPluginEvent()

  object TriggerResetEffect : TestPluginEvent()

  data class TriggerCounterUpdateEffect(val targetValue: Int) : TestPluginEvent()
}

data class TestPluginState(val value: Int = 0)

sealed class TestPluginEffect {
  data class ValueChanged(val newValue: Int) : TestPluginEffect()

  object TriggerParentReset : TestPluginEffect()

  data class TriggerParentCounterUpdate(val targetValue: Int) : TestPluginEffect()
}
