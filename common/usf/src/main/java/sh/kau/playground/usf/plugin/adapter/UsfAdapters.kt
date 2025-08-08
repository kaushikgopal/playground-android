package sh.kau.playground.usf.plugin.adapter

/**
 * Interface for a state adapter that maps plugin state to parent view state.
 *
 * State adapters are the primary mechanism for decoupling plugins from specific parent state types,
 * enabling the same plugin to be used with different view models.
 *
 * @param PluginState The plugin's internal state type
 * @param ParentState The parent state type
 */
fun interface UsfStateAdapter<PluginState, ParentState> {
  /**
   * Apply plugin state to parent state.
   *
   * This method is called whenever the plugin's state changes. It takes the current parent state
   * and the plugin's state, then returns an updated parent state that reflects the plugin state.
   *
   * @param state Current parent state
   * @param pluginState Current plugin state
   * @return Updated parent state reflecting the plugin state
   */
  fun apply(state: ParentState, pluginState: PluginState): ParentState
}

/**
 * Interface for an effect adapter that maps plugin effects to parent effect types.
 *
 * Effect adapters allow plugins to emit their own effect types while letting the parent component
 * decide how to map these to its own effect system.
 *
 * @param PluginEffect The plugin effect type
 * @param ParentEffect The parent effect type
 */
fun interface UsfEffectAdapter<in PluginEffect, out ParentEffect> {
  /**
   * Map a plugin effect to a parent effect type.
   *
   * This method is called whenever the plugin emits an effect. It takes the plugin effect and maps
   * it to a parent effect type, or returns null if this effect should not be propagated to the
   * parent.
   *
   * @param pluginEffect The effect from the plugin
   * @return A parent effect, or null if this effect shouldn't be mapped
   */
  fun map(pluginEffect: PluginEffect): ParentEffect?
}

/**
 * Event mapper interface for filtering and transforming events before they reach a plugin.
 *
 * UsfEventMapper serves three primary purposes in the USF architecture:
 * 1. Event Filtering - Determines which events should be processed by a specific plugin
 * 2. Event Transformation - Transforms events from parent types to plugin-specific types
 * 3. Type Safety - Acts as a bridge between parent event types and strongly-typed plugin events
 *
 * When a plugin is registered with a view model, an event mapper is used to determine if an
 * incoming event should be processed by that plugin. Only if the mapper returns a non-null value
 * will the event be forwarded to the plugin's processEvent method.
 *
 * This pattern allows multiple plugins to selectively handle different event types without needing
 * to expose plugin implementation details to the parent view model.
 *
 * @param ParentEvent The parent event type
 * @param PluginEvent The plugin event type
 */
fun interface UsfEventAdapter<ParentEvent, PluginEvent> {
  /**
   * Maps or filters an incoming parent event to a plugin event type.
   *
   * If this method returns:
   * - non-null: The plugin will process the returned event
   * - null: The plugin will ignore this event
   *
   * @param event The parent event to evaluate
   * @return The mapped plugin event if the plugin should process it, or null if it should be
   *   ignored
   */
  fun map(event: ParentEvent): PluginEvent?
}
