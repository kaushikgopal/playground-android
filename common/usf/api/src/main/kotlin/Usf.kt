package sh.kau.playground.usf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * This interface provides the contract for the ViewModel to follow USF/UDF architecture. Having
 * this as an interface allows us to possibly avoid using of [AndroidViewModel] or [ViewModel] if we
 * don't need it.
 *
 * [Any] type used because of a Kotlin Compiler bug that won't take in @NotNull event: E
 * https://youtrack.jetbrains.com/issue/KT-36770
 */
interface Usf<Event : Any, UiState : Any, Effect : Any> {

  fun processInput(event: Event)

  val uiState: StateFlow<UiState>

  val effects: Flow<Effect>
}
