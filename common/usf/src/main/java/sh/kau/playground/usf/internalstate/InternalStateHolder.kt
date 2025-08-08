package sh.kau.playground.usf.internalstate

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe holder for internal state with atomic updates.
 *
 * @param T The type of state being held
 * @param initialState The initial state value
 */
class InternalStateHolder<T>(initialState: T) {
  private var _state: T = initialState
  private val mutex = Mutex()

  /**
   * Gets the current state value.
   */
  val get: T
    get() = _state

  /**
   * Updates the state atomically using the provided update function.
   *
   * @param update Function that receives the current state and returns the new state
   * @return The new state after update
   */
  suspend fun update(update: (T) -> T): T = mutex.withLock {
    _state = update(_state)
    _state
  }

  /**
   * Updates the state atomically using the provided update function (non-suspending version).
   *
   * @param update Function that receives the current state and returns the new state
   * @return The new state after update
   */
  fun updateBlocking(update: (T) -> T): T = synchronized(this) {
    _state = update(_state)
    _state
  }
}