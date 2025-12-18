package sh.kau.playground.usf.plugin

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
import sh.kau.playground.usf.api.Usf
import sh.kau.playground.usf.inspector.UsfInspector
import sh.kau.playground.usf.plugin.adapter.UsfEffectAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEffectToEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfEventAdapter
import sh.kau.playground.usf.plugin.adapter.UsfStateAdapter

/**
 * Manages plugin registrations and interactions.
 *
 * This class centralizes plugin-related functionality including:
 * - Registration and unregistration of plugins
 * - Setup of state and effect subscriptions
 * - Forwarding events to plugins
 * - Managing plugin lifecycle (start/stop)
 *
 * It can be used by any component that needs to work with UsfPlugins, like ViewModels or other
 * plugins.
 *
 * @param Event The parent component's event type
 * @param State The parent component's state type
 * @param Effect The parent component's effect type
 * @param state The state flow that will be updated with plugin state changes
 * @param coroutineScope The scope used for launching coroutines
 * @param inspector Inspector for logging errors and events
 * @param parentInput Optional callback to send events to the parent component for processing
 */
@OptIn(ExperimentalStdlibApi::class)
internal class UsfPluginRegistrarImpl<Event : Any, State : Any, Effect : Any>(
    override val state: MutableStateFlow<State>,
    private val coroutineScope: CoroutineScope,
    private val inspector: UsfInspector?,
    private val parentInput: (Event) -> Unit = {},
) : Usf<Event, State, Effect>, UsfPluginRegistrar<Event, State, Effect> {
  private var _coroutineScope: CoroutineScope = coroutineScope

  private val _effects = MutableSharedFlow<Effect>()
  override val effects: Flow<Effect> = _effects.asSharedFlow()

  private val _registeredPlugins =
      mutableMapOf<UsfPluginInterface<*, *, *>, PluginRegistration<*, *, *, Event, State, Effect>>()

  /**
   * Cached list of plugins that have event mappers.
   *
   * This cache avoids allocating intermediate collections on every event by maintaining a list of
   * plugins that can receive events. Updated when plugins are registered or unregistered.
   */
  private val _pluginsWithEventMappers =
      mutableListOf<Pair<UsfPluginInterface<*, *, *>, PluginRegistration<*, *, *, Event, State, Effect>>>()

  /**
   * Registers a plugin with its adapters.
   *
   * Plugins can be registered at any time, even before the parent component is ready. If the parent
   * scope is not yet available, the plugin will be stored and set up later when the scope becomes
   * available (deferred registration). This allows plugins to be registered in constructors or init
   * blocks without timing errors.
   *
   * @param plugin The plugin to register
   * @param mapEvent Adapter that filters/maps events for this plugin
   * @param applyState Adapter that maps plugin state to parent state
   * @param mapEffect Adapter that maps plugin effects to parent effects
   * @param transformEffect Adapter that transforms plugin effects to parent events
   */
  override fun <PluginEvent, PluginState, PluginEffect> register(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>,
      mapEvent: UsfEventAdapter<Event, PluginEvent>?,
      applyState: UsfStateAdapter<PluginState, State>?,
      mapEffect: UsfEffectAdapter<PluginEffect, Effect>?,
      transformEffect: UsfEffectToEventAdapter<PluginEffect, Event>?,
  ) {
    if (_registeredPlugins.containsKey(plugin)) return

    val registration = PluginRegistration(applyState, mapEffect, mapEvent, transformEffect)
    _registeredPlugins[plugin] = registration

    // Add to cache if this plugin has an event mapper
    if (registration.eventMapper != null) {
      _pluginsWithEventMappers.add(plugin to registration)
    }

    setupPluginSubscriptions(plugin)
  }

  /**
   * Unregisters a previously registered plugin.
   *
   * If the plugin was never fully set up (deferred registration before scope was available), this
   * will only remove it from the registered plugins list without calling lifecycle methods.
   *
   * @param plugin The plugin to unregister
   */
  override fun <PluginEvent, PluginState, PluginEffect> unregister(
      plugin: UsfPluginInterface<PluginEvent, PluginState, PluginEffect>
  ) {
    val registration = _registeredPlugins.remove(plugin) ?: return

    // Remove from cache if this plugin had an event mapper
    if (registration.eventMapper != null) {
      _pluginsWithEventMappers.removeAll { (p, _) -> p === plugin }
    }

    registration.pluginScope?.cancel()
    plugin.stop()
  }

  /**
   * Processes an event through all registered plugins.
   *
   * Uses the cached list of plugins with event mappers to avoid allocating intermediate collections
   * on every event.
   *
   * @param event The event to process
   */
  @Suppress("UNCHECKED_CAST")
  override fun input(event: Event) {
    // Iterate over cached list to avoid filter allocations on hot path
    _pluginsWithEventMappers.forEach { (plugin, registration) ->
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

    // Collect plugin effects only once and process through both adapters if present
    // This avoids the performance issue of collecting the same flow multiple times
    if (registration.effectAdapter != null || registration.effectToEventAdapter != null) {
      pluginScope.launch {
        plugin.effects.collect { pluginEffect ->
          try {
            // Map plugin effect to parent effect if adapter exists
            registration.effectAdapter?.map(pluginEffect)?.let { effect -> _effects.emit(effect) }

            // Transform plugin effect to parent event if adapter exists
            registration.effectToEventAdapter?.map(pluginEffect)?.let { event ->
              parentInput(event)
            }
          } catch (e: Exception) {
            if (e is CancellationException) throw e // propagate cancellation
            inspector?.error(e, "[pef →  ef|ev]")
          }
        }
      }
    }

    plugin.start(pluginScope)
  }

  internal fun start(coroutineScope: CoroutineScope) {
    _coroutineScope = coroutineScope

    // Rebuild the event mapper cache when restarting with a new scope
    _pluginsWithEventMappers.clear()
    _registeredPlugins.forEach { (plugin, registration) ->
      if (registration.eventMapper != null) {
        _pluginsWithEventMappers.add(plugin to registration)
      }
    }

    _registeredPlugins.keys.forEach { plugin -> setupPluginSubscriptions(plugin) }
  }

  internal fun stop() {
    _registeredPlugins.keys.forEach { plugin -> plugin.stop() }
    _registeredPlugins.values.forEach { registration -> registration.pluginScope?.cancel() }
  }
}

/**
 * Registration wrapper that encapsulates a plugin and its adapters.
 *
 * This class manages the relationship between a plugin and its parent component, storing the
 * adapters needed for state/effect mapping, event filtering, and effect-to-event transformation,
 * along with the isolated plugin scope.
 */
private class PluginRegistration<PluginEvent, PluginState, PluginEffect, Event, State, Effect>(
    val stateAdapter: UsfStateAdapter<PluginState, State>?,
    val effectAdapter: UsfEffectAdapter<PluginEffect, Effect>?,
    val eventMapper: UsfEventAdapter<Event, PluginEvent>?,
    val effectToEventAdapter: UsfEffectToEventAdapter<PluginEffect, Event>?,
    var pluginScope: CoroutineScope? = null,
)
