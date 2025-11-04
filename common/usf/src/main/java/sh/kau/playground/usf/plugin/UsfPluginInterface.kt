/**
 * UsfPluginInterface - Plugin pattern for Unidirectional State Flow architecture
 *
 * UsfPluginInterface enables reuse of logic across multiple view models in the Unidirectional State
 * Flow architecture. This pattern ensures plugins can be composed into different view models
 * without tight coupling, promoting code reuse and maintainability.
 *
 * Benefits:
 * 1. Plugins are decoupled from parent view state types
 * 2. Same plugin can be used with multiple view models
 * 3. Adapters handle state and effect mapping between components
 * 4. Clear separation of concerns
 *
 * Example usage:
 * 1. Create a plugin with its own state type:
 * ```
 * class CartPlugin(
 *     initialState: CartPluginState,
 *     initialScope: CoroutineScope
 * ) : UsfPlugin<CartPluginEvent, CartPluginState, CartPluginEffect>(initialState, initialScope) {
 *
 *     override fun processEvent(event: CartPluginEvent) {
 *         when (event) {
 *             is CartPluginEvent.AddItem -> {
 *                 updateState { currentState ->
 *                     currentState.copy(itemCount = currentState.itemCount + 1)
 *                 }
 *                 emitEffect(CartPluginEffect.ItemAdded(event.itemName))
 *             }
 *         }
 *     }
 * }
 * ```
 * 2. Register the plugin using functional interface adapters:
 * ```
 * // In ProductViewModel
 * register(
 *     plugin = cartPlugin,
 *     adaptState = { parentState, pluginState ->
 *         parentState.copy(
 *             productCount = pluginState.itemCount,
 *             cartTotal = pluginState.total
 *         )
 *     },
 *     adaptEffect = { pluginEffect ->
 *         when (pluginEffect) {
 *             is CartPluginEffect.ItemAdded -> ProductEffect.ShowItemAddedToast(pluginEffect.itemName)
 *             is CartPluginEffect.Checkout -> ProductEffect.NavigateToCheckout
 *             else -> null
 *         }
 *     }
 * )
 *
 * // In CheckoutViewModel
 * register(
 *     plugin = cartPlugin,
 *     adaptState = { parentState, pluginState ->
 *         parentState.copy(
 *             itemCount = pluginState.itemCount,
 *             total = pluginState.total
 *         )
 *     },
 *     adaptEffect = { pluginEffect ->
 *         when (pluginEffect) {
 *             is CartPluginEffect.Checkout -> CheckoutEffect.ProcessPayment
 *             else -> null
 *         }
 *     }
 * )
 * ```
 *
 * This approach creates truly reusable components that can be composed into different view models
 * without tight coupling, with state and effects mapped appropriately to each parent context.
 */
package sh.kau.playground.usf.plugin

import kotlinx.coroutines.CoroutineScope
import sh.kau.playground.usf.api.Usf

/**
 * Core interface for the USF plugin pattern, enabling modular and reusable UI logic.
 *
 * UsfPluginInterface components encapsulate specific pieces of functionality that can be composed
 * into different view models. They maintain their own internal state and can emit effects while
 * being decoupled from specific parent view state types.
 *
 * @param Event The plugin's event type
 * @param State The plugin's internal state type
 * @param Effect The plugin's effect type
 */
interface UsfPluginInterface<Event, State, Effect> : Usf<Event, State, Effect> {

  /**
   * Called when the plugin is first registered with a parent component.
   *
   * This lifecycle method serves two purposes:
   * 1. Setting up the plugin's isolated scope that is a child of the parent scope
   * 2. Performing any initial setup or state initialization
   *
   * The plugin receives an isolated scope that preserves the parent-child cancellation relationship
   * but prevents direct access to the parent scope's context elements. This ensures proper
   * isolation while still allowing cancellation to propagate. The isolated scope preserves only the
   * dispatcher from the parent context.
   *
   * When the parent scope is cancelled, plugin operations will also be cancelled. Override this
   * method to add additional initialization logic.
   *
   * @param coroutineScope The isolated coroutine scope for this plugin
   */
  fun onRegistered(coroutineScope: CoroutineScope)

  /**
   * Called when the plugin is unregistered from its parent component.
   *
   * Override this method to perform cleanup operations such as cancelling ongoing tasks, closing
   * resources, or resetting state. This is called when the plugin is explicitly unregistered or
   * when the parent component's pipeline terminates.
   */
  fun onUnregistered()
}
