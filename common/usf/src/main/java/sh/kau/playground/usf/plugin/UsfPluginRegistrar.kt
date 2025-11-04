package sh.kau.playground.usf.plugin

import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter

/**
 * Interface for components that can register and manage child plugins.
 *
 * This interface defines the contract for registering and unregistering plugins, allowing for
 * composition of plugin functionality. Implementers are responsible for properly managing plugin
 * lifecycle and mapping state/effects between parent and child plugins.
 *
 * @param Event The parent component's event type
 * @param State The parent component's state type
 * @param Effect The parent component's effect type
 */
interface UsfPluginRegistrar<Event : Any, State : Any, Effect : Any> {

  /**
   * Registers a child plugin with this component.
   *
   * The child plugin's state and effects will be mapped to this component's state and effects using
   * the provided adapters. Events can be filtered and mapped to the child plugin using the event
   * mapper.
   *
   * @param plugin The child plugin to register
   * @param mapEvent Optional mapper to filter and transform events for the child plugin. If null,
   *   no events will be sent to this plugin.
   * @param applyState Optional adapter to map child state to parent state. If null, state changes
   *   from this plugin will not affect parent state.
   * @param mapEffect Optional adapter to map child effects to parent effects. If null, effects from
   *   this plugin will not be propagated to parent.
   * @param transformEffect Optional adapter to transform child effects to parent events. If null,
   *   child effects will not trigger parent events.
   */
  fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      mapEvent: UsfEventAdapter<Event, PluginEvent>? = null,
      applyState: UsfStateAdapter<PluginState, State>? = null,
      mapEffect: UsfEffectAdapter<PluginEffect, Effect>? = null,
      transformEffect: UsfEffectToEventAdapter<PluginEffect, Event>? = null,
  )

  /**
   * Unregisters a previously registered plugin.
   *
   * This stops the child plugin from receiving events and removes its state/effect mapping from
   * this component.
   *
   * @param plugin The child plugin to unregister
   */
  fun <PluginEvent, PluginState, PluginEffect> unregister(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  )
}
