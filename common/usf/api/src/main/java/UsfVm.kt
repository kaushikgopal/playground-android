package sh.kau.playground.usf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UsfVm<Event : Any, UiState : Any, Effect : Any> {

  fun processInput(event: Event)

  val uiState: StateFlow<UiState>

  val effects: Flow<Effect>
}
