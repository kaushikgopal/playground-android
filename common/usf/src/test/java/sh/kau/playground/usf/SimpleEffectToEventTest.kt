package sh.kau.playground.usf

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter

class SimpleEffectToEventTest {

  @Test
  fun testEffectToEventAdapter() = runTest {
    // Test the basic functionality of the UsfEffectToEventAdapter
    val adapter =
        UsfEffectToEventAdapter<TestPluginEffect, TestEvent> { pluginEffect ->
          when (pluginEffect) {
            is TestPluginEffect.TriggerParentReset -> TestEvent.PluginTriggeredReset
            is TestPluginEffect.TriggerParentCounterUpdate ->
                TestEvent.PluginTriggeredCounterUpdate(pluginEffect.targetValue)
            else -> null
          }
        }

    // Test transformation
    val resetEvent = adapter.map(TestPluginEffect.TriggerParentReset)
    assertEquals(TestEvent.PluginTriggeredReset, resetEvent)

    val updateEvent = adapter.map(TestPluginEffect.TriggerParentCounterUpdate(42))
    assertEquals(TestEvent.PluginTriggeredCounterUpdate(42), updateEvent)

    val filteredEvent = adapter.map(TestPluginEffect.ValueChanged(10))
    assertEquals(null, filteredEvent)
  }
}
