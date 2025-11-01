package app.pudi.android.usf.viewmodel

import sh.kau.playground.usf.viewmodel.UsfViewModel

/**
 * USF Event Callback Utilities
 *
 * These utilities solve a critical Compose performance problem: **reference stability**.
 *
 * ## The Problem: Unstable Direct Calls
 *
 * When you call `viewModel.input()` directly in a Composable, you create a NEW lambda on EVERY
 * recomposition:
 * ```kotlin
 * @Composable
 * fun MyScreen(viewModel: MyViewModel) {
 *     val uiState by viewModel.state.collectAsState()
 *
 *     // ❌ PROBLEM: New lambda created on every recomposition
 *     OutlinedTextField(
 *         value = uiState.text,
 *         onValueChange = { text -> viewModel.input(Event.TextChanged(text)) }
 *         //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *         //              This is a NEW object reference each time MyScreen recomposes
 *     )
 * }
 * ```
 *
 * **What happens:**
 * 1. User types a character → `uiState` updates → MyScreen recomposes
 * 2. New `onValueChange` lambda is created with a different reference
 * 3. TextField sees a "different" callback → may trigger unnecessary recomposition
 * 4. For high-frequency callbacks (text input, sliders), this compounds into input lag
 *
 * ## The Solution: Callbacks in State
 *
 * Store callbacks in `UiState` where they're created ONCE and preserved through all state updates:
 * ```kotlin
 * // ✅ SOLUTION: Callback created once in initialState()
 * data class MyUiState(
 *     val text: String = "",
 *     val onTextChanged: (String) -> Unit = {}
 * )
 *
 * override fun initialState() = MyUiState(
 *     onTextChanged = inputEventCallback(Event::TextChanged)
 *     //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *     //              Created ONCE, preserved through all .copy() operations
 * )
 *
 * // When state updates:
 * updateState { it.copy(text = "new") }  // onTextChanged reference stays the same ✅
 *
 * // In screen - always the same reference
 * OutlinedTextField(
 *     value = uiState.text,
 *     onValueChange = uiState.onTextChanged  // Stable reference ✅
 * )
 * ```
 *
 * **Why this works:**
 * - Callback is created once in `initialState()`
 * - Kotlin's data class `.copy()` preserves the callback reference
 * - Compose sees the same reference across recompositions
 * - No unnecessary recomposition of child components
 *
 * ## When to Use These Utilities
 *
 * **Use for high-frequency callbacks:**
 * - ✅ TextField `onValueChange` (called on every keystroke)
 * - ✅ Slider `onValueChange` (called continuously during drag)
 * - ✅ `onFocusChanged` (frequent during user interaction)
 * - ✅ Drag/scroll handlers (very high frequency)
 *
 * **Skip for low-frequency actions:**
 * - ❌ Button clicks (direct `viewModel.input()` is fine)
 * - ❌ Simple one-off actions (instability doesn't matter)
 *
 * ## Performance Impact
 * | Approach                  | Stability  | Recomposition | Best For              |
 * |---------------------------|------------|---------------|-----------------------|
 * | Callbacks in state (this) | ✅ Stable   | None          | High-frequency inputs |
 * | Direct + remember         | ✅ Stable   | None          | Medium complexity     |
 * | Direct (unwrapped)        | ❌ Unstable | Potential     | Low-frequency clicks  |
 */

/**
 * Creates a callback function that emits the specified event when invoked.
 *
 * Use for simple events without parameters (button clicks, simple actions).
 *
 * Example:
 * ```kotlin
 * data class MyUiState(
 *     val onBackClicked: () -> Unit = {}
 * )
 *
 * override fun initialState() = MyUiState(
 *     onBackClicked = inputEventCallback(MyEvent.BackClicked)
 * )
 * ```
 *
 * @param event The event to emit when the callback is invoked
 * @param inputProcessor Function that processes the event (typically ViewModel::input)
 * @return A stable callback function that emits the event
 */
fun <Event> inputEventCallback(event: Event, inputProcessor: (Event) -> Unit): () -> Unit = {
  inputProcessor(event)
}

/**
 * Creates a callback function that emits an event with a parameter when invoked.
 *
 * Use for callbacks that need to transform UI parameters into events (text input, selections). This
 * is the most common use case for achieving reference stability in Compose.
 *
 * Example:
 * ```kotlin
 * data class MyUiState(
 *     val searchText: String = "",
 *     val onSearchTextChanged: (String) -> Unit = {}
 * )
 *
 * override fun initialState() = MyUiState(
 *     onSearchTextChanged = inputEventCallback(MyEvent::SearchTextChanged)
 * )
 *
 * // In screen
 * OutlinedTextField(
 *     value = uiState.searchText,
 *     onValueChange = uiState.onSearchTextChanged  // Stable ✅
 * )
 * ```
 *
 * @param eventFactory Function that creates an event from the parameter
 * @param inputProcessor Function that processes the event (typically ViewModel::input)
 * @return A stable callback function that takes a parameter and emits the corresponding event
 */
fun <Param, Event> inputEventCallback(
    eventFactory: (Param) -> Event,
    inputProcessor: (Event) -> Unit,
): (Param) -> Unit = { param -> inputProcessor(eventFactory(param)) }

/**
 * Extension function for UsfViewModel to create simple event callbacks conveniently.
 *
 * Example:
 * ```kotlin
 * override fun initialState() = MyUiState(
 *     onBackClicked = inputEventCallback(MyEvent.BackClicked)
 * )
 * ```
 */
fun <Event : Any, UiState : Any, Effect : Any> UsfViewModel<Event, UiState, Effect>
    .inputEventCallback(event: Event): () -> Unit = inputEventCallback(event, ::input)

/**
 * Extension function for UsfViewModel to create parameterized event callbacks conveniently.
 *
 * This is the primary method for creating stable callbacks for text inputs, selections, and other
 * high-frequency Compose interactions.
 *
 * Example:
 * ```kotlin
 * override fun initialState() = MyUiState(
 *     onTextChanged = inputEventCallback(MyEvent::TextChanged),
 *     onItemSelected = inputEventCallback(MyEvent::ItemSelected),
 *     onFocusChanged = inputEventCallback(MyEvent::FocusChanged)
 * )
 * ```
 */
fun <Param, Event : Any, UiState : Any, Effect : Any> UsfViewModel<Event, UiState, Effect>
    .inputEventCallback(eventFactory: (Param) -> Event): (Param) -> Unit =
    inputEventCallback(eventFactory, ::input)
