package sh.kau.playground.usf

import sh.kau.playground.usf.plugin.UsfPlugin
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter
import sh.kau.playground.usf.scope.ResultScope

/** Test plugin for effect-to-event transformation testing. */
class TestEffectToEventPlugin : UsfPlugin<TestPluginEvent, TestPluginState, TestPluginEffect>() {

  val emittedEffects = mutableListOf<TestPluginEffect>()

  override fun initialState(): TestPluginState = TestPluginState()

  override suspend fun ResultScope<TestPluginState, TestPluginEffect>.process(
      event: TestPluginEvent
  ) {
    when (event) {
      is TestPluginEvent.Increment -> {
        var newValue = 0
        updateState {
          val updated = it.copy(value = it.value + 1)
          newValue = updated.value
          updated
        }
        val effect = TestPluginEffect.ValueChanged(newValue)
        emittedEffects.add(effect)
        emitEffect(effect)
      }
      is TestPluginEvent.TriggerResetEffect -> {
        val effect = TestPluginEffect.TriggerParentReset
        emittedEffects.add(effect)
        emitEffect(effect)
      }
      is TestPluginEvent.TriggerCounterUpdateEffect -> {
        val effect = TestPluginEffect.TriggerParentCounterUpdate(event.targetValue)
        emittedEffects.add(effect)
        emitEffect(effect)
      }
    }
  }

  // Helper methods for creating adapters in tests
  fun createTestStateAdapter(): UsfStateAdapter<TestPluginState, TestState> {
    return UsfStateAdapter { parentState, pluginState ->
      parentState.copy(counter = pluginState.value)
    }
  }

  fun createTestEffectAdapter(): UsfEffectAdapter<TestPluginEffect, TestEffect> {
    return UsfEffectAdapter { pluginEffect ->
      when (pluginEffect) {
        is TestPluginEffect.ValueChanged -> TestEffect.CounterUpdated(pluginEffect.newValue)
        is TestPluginEffect.TriggerParentReset,
        is TestPluginEffect.TriggerParentCounterUpdate -> null // Handled by effect-to-event
      }
    }
  }

  fun createTestEffectToEventAdapter(): UsfEffectToEventAdapter<TestPluginEffect, TestEvent> {
    return UsfEffectToEventAdapter { pluginEffect ->
      when (pluginEffect) {
        is TestPluginEffect.TriggerParentReset -> TestEvent.PluginTriggeredReset
        is TestPluginEffect.TriggerParentCounterUpdate ->
            TestEvent.PluginTriggeredCounterUpdate(pluginEffect.targetValue)
        is TestPluginEffect.ValueChanged -> null // Not transformed to event
      }
    }
  }
}
