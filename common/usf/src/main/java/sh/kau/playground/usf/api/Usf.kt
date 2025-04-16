package sh.kau.playground.usf.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * This interface provides the contract for the ViewModel to follow USF/UDF architecture. Having
 * this as an interface allows us to possibly avoid using of [AndroidViewModel] or [ViewModel] if we
 * don't need it.
 *
 * We use `: Any` on the Type declarations because generics standalone are considered nullable so
 * they would be the equivalent of `: Any?` by default which is not what we want.
 */
interface Usf<Input : Any, UiState : Any, Effect : Any> {

  fun processInput(input: Input)

  val uiState: StateFlow<UiState>

  val effects: Flow<Effect>
}
