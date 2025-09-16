package sh.kau.playground.usf

import sh.kau.playground.usf.plugin.UsfPluginRegistrarImpl
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleEffectToEventTest {

    @Test
    fun testEffectToEventAdapter() = runTest {
        // Test the basic functionality of the UsfEffectToEventAdapter
        val adapter = UsfEffectToEventAdapter<TestPluginEffect, TestEvent> { pluginEffect ->
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

    @Test
    fun testPluginRegistrarWithEffectToEvent() = runTest {
        val capturedEvents = mutableListOf<TestEvent>()
        val state = MutableStateFlow(TestState())

        val registrar = UsfPluginRegistrarImpl<TestEvent, TestState, TestEffect>(
            state = state,
            coroutineScope = backgroundScope,
            inspector = null,
            parentInput = { event -> capturedEvents.add(event) }
        )

        // Create a simple test plugin
        val plugin = TestEffectToEventPlugin()

        // Register with effect-to-event adapter
        registrar.register(
            plugin = plugin,
            mapEvent = null,
            applyState = null,
            mapEffect = null,
            transformEffect = UsfEffectToEventAdapter { pluginEffect ->
                when (pluginEffect) {
                    is TestPluginEffect.TriggerParentReset -> TestEvent.PluginTriggeredReset
                    else -> null
                }
            }
        )

        // Trigger plugin effect
        plugin.input(TestPluginEvent.TriggerResetEffect)

        // Wait a bit for async processing
        testScheduler.advanceUntilIdle()

        // Verify event was captured
        assertEquals(1, capturedEvents.size)
        assertEquals(TestEvent.PluginTriggeredReset, capturedEvents[0])
    }
}