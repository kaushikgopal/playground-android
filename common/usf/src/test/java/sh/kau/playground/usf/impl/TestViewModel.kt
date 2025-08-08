package sh.kau.playground.usf.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.kau.playground.usf.TestEffect
import sh.kau.playground.usf.TestEvent
import sh.kau.playground.usf.TestState
import sh.kau.playground.usf.inspector.NoOpInspector
import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.scope.ResultScope
import sh.kau.playground.usf.viewmodel.UsfViewModel

open class TestViewModel(
    private val customInitialStateProvider: (() -> TestState)? = null,
    private val testInspector: UsfInspector = NoOpInspector,
    coroutineScope: CoroutineScope,
    processingDispatcher: CoroutineDispatcher
) :
    UsfViewModel<TestEvent, TestState, TestEffect>(
        inspector = testInspector,
        coroutineScope = coroutineScope,
        processingDispatcher = processingDispatcher,
    ) {
  var throwErrorInProcessEvent = false

  override fun initialState(): TestState {
    return customInitialStateProvider?.invoke() ?: TestState()
  }

  @Suppress("UseCheckOrError")
  override suspend fun ResultScope<TestState, TestEffect>.process(event: TestEvent) {
    if (throwErrorInProcessEvent && event is TestEvent.EventThatCausesError) {
      throw IllegalStateException("Test error in processEvent (synchronous part)")
    }

    when (event) {
      is TestEvent.IncrementCounter -> {
        updateState { it.copy(counter = it.counter + 1) }
      }
      is TestEvent.UpdateName -> {
        updateState { it.copy(name = event.name) }
      }

      is TestEvent.EmitEffect -> {
        emitEffect(TestEffect.SimpleEffect)
      }

      is TestEvent.ComplexEvent -> {
        updateState { it.copy(counter = it.counter + event.value) }
        emitEffect(TestEffect.NamedEffect(event.label))
      }

      is TestEvent.EventThatCausesError -> {
        updateState { it.copy(name = "Error Event Processed (sync)") }
      }

      is TestEvent.AsyncOperationEvent -> {
        coroutineScope.launch {
          try {
            delay(event.delayMillis)
            if (event.shouldThrowError) {
              throw IllegalStateException("Simulated error in async operation")
            }
            updateState { it.copy(counter = event.targetCounter) }
            event.newName?.let { name -> updateState { it.copy(name = name) } }
            event.effectToEmit?.let { emitEffect(it) }
          } catch (e: IllegalStateException) {
            if (e.message == "Simulated error in async operation") {
              testInspector.error(
                  e,
                  "[TestViewModel] Caught simulated async error as expected.",
              )
            } else {
              throw e
            }
          }
        }
      }
    }
  }
}
