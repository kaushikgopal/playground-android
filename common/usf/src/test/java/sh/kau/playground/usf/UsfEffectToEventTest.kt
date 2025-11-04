package sh.kau.playground.usf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel

class UsfEffectToEventTest {

  @Test
  @DisplayName("Should transform plugin effects to parent events and trigger parent state updates")
  fun testPluginEffectToEventTransformation() = runTest {
    // Arrange
    val viewModel = TestEffectToEventViewModel(backgroundScope)
    val plugin = TestEffectToEventPlugin()

    // Register plugin with effect-to-event transformation
    viewModel.registerTestPlugin(plugin)

    val states = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Initial state check
    assertEquals(1, states.size)
    assertEquals(0, states.last().counter)

    // Act: Trigger plugin effect that should be transformed to parent event
    plugin.input(TestPluginEvent.TriggerResetEffect)
    advanceUntilIdle()

    // Assert: Plugin effect should be emitted
    assertEquals(1, plugin.emittedEffects.size)
    assertTrue(plugin.emittedEffects[0] is TestPluginEffect.TriggerParentReset)

    // Assert: Parent should have processed the transformed event
    assertEquals(1, viewModel.processedEvents.size)
    assertEquals(TestEvent.PluginTriggeredReset, viewModel.processedEvents[0])

    // Assert: Parent effect should be emitted
    assertEquals(1, effects.size)
    assertTrue(effects.last() is TestEffect.CounterUpdated)
    assertEquals(0, (effects.last() as TestEffect.CounterUpdated).value)
  }

  @Test
  @DisplayName("Should transform plugin effects with parameters to parent events")
  fun testPluginEffectToEventTransformationWithParameters() = runTest {
    // Arrange
    val viewModel = TestEffectToEventViewModel(backgroundScope)
    val plugin = TestEffectToEventPlugin()

    viewModel.registerTestPlugin(plugin)

    val states = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act: Process a plugin event that will emit TriggerParentCounterUpdate effect
    plugin.input(TestPluginEvent.TriggerCounterUpdateEffect(42))
    advanceUntilIdle()

    // Assert: Plugin effect should be emitted
    assertEquals(1, plugin.emittedEffects.size)
    assertTrue(plugin.emittedEffects[0] is TestPluginEffect.TriggerParentCounterUpdate)
    val effect = plugin.emittedEffects[0] as TestPluginEffect.TriggerParentCounterUpdate
    assertEquals(42, effect.targetValue)

    // Assert: Parent should have processed the transformed event with parameter
    assertEquals(1, viewModel.processedEvents.size)
    assertTrue(viewModel.processedEvents[0] is TestEvent.PluginTriggeredCounterUpdate)
    val event = viewModel.processedEvents[0] as TestEvent.PluginTriggeredCounterUpdate
    assertEquals(42, event.newValue)

    // Assert: Parent state should be updated with the parameter value
    assertEquals(2, states.size)
    assertEquals(42, states.last().counter)

    // Assert: Parent effect should be emitted with the correct value
    assertEquals(1, effects.size)
    assertTrue(effects.last() is TestEffect.CounterUpdated)
    assertEquals(42, (effects.last() as TestEffect.CounterUpdated).value)
  }

  @Test
  @DisplayName("Should not transform plugin effects when effect-to-event adapter returns null")
  fun testPluginEffectToEventTransformationFiltering() = runTest {
    // Arrange
    val viewModel = TestEffectToEventViewModel(backgroundScope)
    val plugin = TestEffectToEventPlugin()

    viewModel.registerTestPlugin(plugin)

    val states = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act: Process a plugin event that emits an effect that's NOT transformed to an event
    plugin.input(TestPluginEvent.Increment)
    advanceUntilIdle()

    // Assert: Plugin effect should be emitted but not transformed to parent event
    assertEquals(1, plugin.emittedEffects.size)
    assertTrue(plugin.emittedEffects[0] is TestPluginEffect.ValueChanged)

    // Assert: Parent should NOT have processed any transformed event
    assertEquals(0, viewModel.processedEvents.size)

    // Assert: Only effect-to-effect transformation should occur
    assertEquals(1, effects.size)
    assertTrue(effects.last() is TestEffect.CounterUpdated)

    // Assert: Parent state should be updated only by plugin state mapping
    assertEquals(2, states.size)
    assertEquals(1, states.last().counter) // Updated via state adapter
  }

  @Test
  @DisplayName("Should handle multiple plugin effects with mixed transformation")
  fun testMultiplePluginEffectsWithMixedTransformation() = runTest {
    // Arrange
    val viewModel = TestEffectToEventViewModel(backgroundScope)
    val plugin = TestEffectToEventPlugin()

    viewModel.registerTestPlugin(plugin)

    val states = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act: Process multiple events - some transform to events, some don't
    plugin.input(TestPluginEvent.Increment) // Should not transform to event
    plugin.input(TestPluginEvent.TriggerCounterUpdateEffect(100)) // Should transform
    plugin.input(TestPluginEvent.Increment) // Should not transform to event
    advanceUntilIdle()

    // Assert: All plugin effects should be emitted
    assertEquals(3, plugin.emittedEffects.size)

    // Assert: Only one event should be transformed
    assertEquals(1, viewModel.processedEvents.size)
    assertTrue(viewModel.processedEvents[0] is TestEvent.PluginTriggeredCounterUpdate)

    // Assert: Multiple parent effects should be emitted (2 from effect mapping + 1 from event
    // processing)
    assertEquals(3, effects.size)

    // Assert: Final state should reflect the transformed event value (100) not plugin increments
    assertTrue(states.last().counter == 100)
  }

  @Test
  @DisplayName("Should handle plugin registration without effect-to-event adapter")
  fun testPluginRegistrationWithoutEffectToEventAdapter() = runTest {
    // Arrange
    val viewModel = TestEffectToEventViewModel(backgroundScope)
    val plugin = TestEffectToEventPlugin()

    // Register plugin WITHOUT effect-to-event transformation
    viewModel.registerTestPluginWithoutEffectToEvent(plugin)

    val states = mutableListOf<TestState>()
    val effects = mutableListOf<TestEffect>()

    backgroundScope.launch { viewModel.state.toList(states) }
    backgroundScope.launch { viewModel.effects.toList(effects) }
    runCurrent()

    // Act: Try to trigger effect that would normally be transformed to event
    plugin.input(TestPluginEvent.TriggerResetEffect)
    advanceUntilIdle()

    // Assert: Plugin effect should be emitted
    assertEquals(1, plugin.emittedEffects.size)
    assertTrue(plugin.emittedEffects[0] is TestPluginEffect.TriggerParentReset)

    // Assert: Parent should NOT have processed any transformed event
    assertEquals(0, viewModel.processedEvents.size)

    // Assert: No parent effects should be emitted (since no effect-to-event transformation)
    assertEquals(0, effects.size)

    // Assert: State should remain unchanged
    assertEquals(1, states.size) // Only initial state
  }

  /** Test ViewModel that tracks processed events for verification */
  private class TestEffectToEventViewModel(coroutineScope: CoroutineScope) :
      UsfViewModel<TestEvent, TestState, TestEffect>(coroutineScope) {

    val processedEvents = mutableListOf<TestEvent>()

    override fun initialState(): TestState = TestState()

    override suspend fun ResultScope<TestState, TestEffect>.process(event: TestEvent) {
      processedEvents.add(event)
      when (event) {
        is TestEvent.PluginTriggeredReset -> {
          updateState { it.copy(counter = 0) }
          emitEffect(TestEffect.CounterUpdated(0))
        }
        is TestEvent.PluginTriggeredCounterUpdate -> {
          updateState { it.copy(counter = event.newValue) }
          emitEffect(TestEffect.CounterUpdated(event.newValue))
        }
        else -> {
          // Handle other events if needed
        }
      }
    }

    fun registerTestPlugin(plugin: TestEffectToEventPlugin) {
      register(
          plugin = plugin,
          mapEvent = null, // No event mapping for this test
          applyState = plugin.createTestStateAdapter(),
          mapEffect = plugin.createTestEffectAdapter(),
          transformEffect = plugin.createTestEffectToEventAdapter(),
      )
    }

    fun registerTestPluginWithoutEffectToEvent(plugin: TestEffectToEventPlugin) {
      register(
          plugin = plugin,
          mapEvent = null,
          applyState = plugin.createTestStateAdapter(),
          mapEffect = plugin.createTestEffectAdapter(),
          transformEffect = null, // No effect-to-event transformation
      )
    }
  }
}
