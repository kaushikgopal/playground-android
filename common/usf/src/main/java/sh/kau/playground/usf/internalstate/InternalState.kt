package sh.kau.playground.usf.internalstate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate property for managing internal state in USF ViewModels and plugins.
 *
 * This delegate provides a lazy-initialized, thread-safe internal state holder
 * that can be used for managing ViewModel-specific state separate from UI state.
 *
 * Usage:
 * ```
 * private val internalState by internalStateOf {
 *     MyInternalState(
 *         loading = false,
 *         data = emptyList()
 *     )
 * }
 * ```
 *
 * @param T The type of internal state
 * @param initializer Function that provides the initial state
 */
class InternalState<T>(
    private val initializer: () -> T
) : ReadOnlyProperty<Any?, InternalStateHolder<T>> {

  private var _holder: InternalStateHolder<T>? = null

  override fun getValue(thisRef: Any?, property: KProperty<*>): InternalStateHolder<T> {
    return _holder ?: synchronized(this) {
      _holder ?: InternalStateHolder(initializer()).also { _holder = it }
    }
  }

  companion object {
    /**
     * Creates an internal state delegate with the provided initializer.
     *
     * @param initializer Function that provides the initial state
     * @return A delegate property for internal state management
     */
    fun <T> internalStateOf(initializer: () -> T): InternalState<T> {
      return InternalState(initializer)
    }
  }
}