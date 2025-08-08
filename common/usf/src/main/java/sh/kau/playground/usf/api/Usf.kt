package sh.kau.playground.usf.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Usf (Unidirectional State Flow) - Core interface for the USF architecture pattern.
 *
 * USF implements unidirectional data flow for predictable state management: Events → Processing →
 * State Updates → Rendering → User Actions → Events...
 *
 * ## Core Components
 * 1. **Events**: User interactions or system events
 *     - Represented by [Event] type parameter
 *     - Processed by [processEvent] method
 * 2. **State**: Immutable data representing current UI state
 *     - Represented by [State] type parameter
 *     - Exposed through [state] StateFlow
 *     - Single source of truth
 * 3. **Effects**: One-time side effects (navigation, toasts, etc.)
 *     - Represented by [Effect] type parameter
 *     - Exposed through [effects] Flow
 *
 * ## Event Processing Pipeline
 * - Events → State Updates + Effects
 * - One event can produce multiple state updates
 * - One event can produce multiple effects
 *
 * @param Event The type representing user actions or system events
 * @param State The type representing UI state (typically immutable)
 * @param Effect The type representing one-time side effects
 */
interface Usf<Event, State, Effect> {
  /**
   * Processes incoming events to update state or emit effects.
   *
   * @param event The event to process
   */
  fun input(event: Event)

  /** The current USF state as a StateFlow. */
  val state: StateFlow<State>

  /** One-time side effects as a Flow. */
  val effects: Flow<Effect>
}
