package sh.kau.playground.usf.scope

/**
 * Scope interface that provides methods for updating state and emitting effects.
 *
 * ResultScope is used as a receiver type for event processing methods in USF ViewModels and
 * plugins, providing a clean API for state management and effect emission.
 *
 * @param State The state type
 * @param Effect The effect type
 */
interface ResultScope<State, Effect> {

  /**
   * Updates the current state using the provided update function.
   *
   * @param update Function that receives the current state and returns the new state
   */
  fun updateState(update: (State) -> State)

  /**
   * Emits an effect.
   *
   * @param effect The effect to emit
   */
  fun emitEffect(effect: Effect)

  /**
   * Offloads heavy or blocking work to a background dispatcher before returning to the caller.
   *
   * Implementations provide the dispatcher choice; callers simply supply the workload that must not
   * run on the main thread. When the block completes, execution resumes on the original context
   * (typically main) so UI updates remain safe.
   */
  suspend fun <T> offload(block: suspend () -> T): T
}
