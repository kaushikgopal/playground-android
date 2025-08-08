package sh.kau.playground.usf.plugin

import sh.kau.playground.usf.api.Usf
import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages plugin registrations and interactions.
 *
 * This class centralizes plugin-related functionality including:
 * - Registration and unregistration of plugins
 * - Setup of state and effect subscriptions
 * - Forwarding events to plugins
 * - Managing plugin lifecycle (onRegister/onUnregister)
 *
 * It can be used by any component that needs to work with UsfPlugins, like ViewModels or other
 * plugins.
 *
 * @param Event The parent component's event type
 * @param State The parent component's state type
 * @param Effect The parent component's effect type
 * @param state The state flow that will be updated with plugin state changes
 * @param coroutineScope The scope used for launching coroutines
 */
@OptIn(ExperimentalStdlibApi::class)
internal class UsfPluginRegistrarImpl<Event : Any, State : Any, Effect : Any>(
    override val state: MutableStateFlow<State>,
    private val coroutineScope: CoroutineScope,
    private val inspector: UsfInspector?,
) : Usf<Event, State, Effect>, UsfPluginRegistrar<Event, State, Effect> {
  private var _coroutineScope: CoroutineScope = coroutineScope

  private val _effects = MutableSharedFlow<Effect>()
  override val effects: Flow<Effect> = _effects.asSharedFlow()

  private val _registeredPlugins =
      mutableMapOf<UsfPluginInterface<*, *, *>, PluginRegistration<*, *, *, Event, State, Effect>>()

  /**
   * Registers a plugin with its adapters.
   *
   * @param plugin The plugin to register
   * @param adaptEvent Adapter that filters/transforms events for this plugin
   * @param adaptState Adapter that maps plugin state to parent state
   * @param adaptEffect Adapter that maps plugin effects to parent effects
   */
  override fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      adaptEvent: UsfEventAdapter<Event, PluginEvent>?,
      adaptState: UsfStateAdapter<PluginState, State>?,
      adaptEffect: UsfEffectAdapter<PluginEffect, Effect>?,
  ) {
    if (_registeredPlugins.containsKey(plugin)) return

    val registration = PluginRegistration(adaptState, adaptEffect, adaptEvent)
    _registeredPlugins[plugin] = registration

    setupPluginSubscriptions(plugin)
  }

  /**
   * Unregisters a previously registered plugin.
   *
   * @param plugin The plugin to unregister
   */
  override fun <PluginEvent, PluginState, PluginEffect> unregister(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  ) {
    val registration = _registeredPlugins.remove(plugin) ?: return
    registration.pluginScope?.cancel()
    plugin.onUnregistered()
  }

  /**
   * Processes an event through all registered plugins.
   *
   * @param event The event to process
   */
  @Suppress("UNCHECKED_CAST")
  override fun input(event: Event) {
    _registeredPlugins
        .filter { (_, registration) -> registration.eventMapper != null }
        .forEach { (plugin, registration) ->
          val eventMapper = registration.eventMapper as UsfEventAdapter<Event, Any>
          eventMapper.map(event)?.let { mappedEvent -> tryProcessEvent(plugin, mappedEvent) }
        }
  }

  @Suppress("UNCHECKED_CAST")
  private fun tryProcessEvent(plugin: UsfPluginInterface<*, *, *>, mappedEvent: Any) {
    try {
      val typedPlugin = plugin as UsfPluginInterface<Any, *, *>
      typedPlugin.input(mappedEvent)
    } catch (e: Exception) {
      if (e is CancellationException) throw e // propagate cancellation
      inspector?.error(e, "[ev →  s|ef]")
    }
  }

  /**
   * Creates an isolated plugin scope that is a child of the parent scope.
   *
   * This method creates a supervised child scope to ensure that errors in a plugin don't crash the
   * parent, but the plugin's operations are still cancelled when the parent scope is cancelled.
   *
   * @param parentScope The parent coroutine scope
   * @return A new isolated scope with parent-child supervision relationship
   */
  private fun createPluginScope(parentScope: CoroutineScope): CoroutineScope {
    val parentJob = parentScope.coroutineContext[Job]
    val dispatcher = parentScope.coroutineContext[CoroutineDispatcher] ?: Dispatchers.Default
    val supervisedJob = SupervisorJob(parentJob)
    return CoroutineScope(dispatcher + supervisedJob)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <PluginEvent, PluginState, PluginEffect> setupPluginSubscriptions(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  ) {
    val registration =
        _registeredPlugins[plugin]
            as? PluginRegistration<PluginEvent, PluginState, PluginEffect, Event, State, Effect>
            ?: return

    registration.pluginScope?.cancel()

    val pluginScope = createPluginScope(_coroutineScope)
    registration.pluginScope = pluginScope

    if (registration.stateAdapter != null) {
      pluginScope.launch {
        plugin.state.collect { pluginState ->
          try {
            state.update { currentState ->
              registration.stateAdapter.apply(currentState, pluginState)
            }
          } catch (e: Exception) {
            if (e is CancellationException) throw e // propagate cancellation
            inspector?.error(e, "[ps →  s]")
          }
        }
      }
    }

    if (registration.effectAdapter != null) {
      pluginScope.launch {
        plugin.effects.collect { pluginEffect ->
          try {
            registration.effectAdapter.map(pluginEffect)?.let { effect -> _effects.emit(effect) }
          } catch (e: Exception) {
            if (e is CancellationException) throw e // propagate cancellation
            inspector?.error(e, "[pef →  ef]")
          }
        }
      }
    }

    plugin.onRegistered(pluginScope)
  }

  internal fun register(coroutineScope: CoroutineScope) {
    _coroutineScope = coroutineScope
    _registeredPlugins.keys.forEach { plugin -> setupPluginSubscriptions(plugin) }
  }

  internal fun unregister() {
    _registeredPlugins.keys.forEach { plugin -> plugin.onUnregistered() }
    _registeredPlugins.values.forEach { registration -> registration.pluginScope?.cancel() }
  }
}

/**
 * Registration wrapper that encapsulates a plugin and its adapters.
 *
 * This class manages the relationship between a plugin and its parent component, storing the
 * adapters needed for state/effect mapping and event filtering, along with the isolated plugin
 * scope.
 */
private class PluginRegistration<PluginEvent, PluginState, PluginEffect, Event, State, Effect>(
    val stateAdapter: UsfStateAdapter<PluginState, State>?,
    val effectAdapter: UsfEffectAdapter<PluginEffect, Effect>?,
    val eventMapper: UsfEventAdapter<Event, PluginEvent>?,
    var pluginScope: CoroutineScope? = null
)
