package sh.kau.playground.usf.viewmodel

/**
 * Creates a callback function that emits the specified event when invoked.
 *
 * This is typically used in UI state classes to create callbacks for user interactions.
 * The callback will send the event to the ViewModel's input processing pipeline.
 *
 * Usage:
 * ```
 * data class MyUiState(
 *     val onBackClicked: () -> Unit = inputCallback(MyEvent.BackClicked),
 *     val onSubmit: () -> Unit = inputCallback(MyEvent.Submit)
 * )
 * ```
 *
 * @param event The event to emit when the callback is invoked
 * @param inputProcessor Function that processes the event (typically ViewModel::input)
 * @return A callback function that emits the event
 */
fun <Event> inputCallback(
    event: Event,
    inputProcessor: (Event) -> Unit
): () -> Unit = {
    inputProcessor(event)
}

/**
 * Creates a callback function that emits an event with a parameter when invoked.
 *
 * This is typically used in UI state classes for callbacks that need to pass data.
 * The callback will create an event using the provided factory function and send it
 * to the ViewModel's input processing pipeline.
 *
 * Usage:
 * ```
 * data class MyUiState(
 *     val onTextChanged: (String) -> Unit = inputCallbackWithParam(
 *         eventFactory = MyEvent::TextChanged,
 *         inputProcessor = ::processInput
 *     ),
 *     val onItemSelected: (Int) -> Unit = inputCallbackWithParam(
 *         eventFactory = MyEvent::ItemSelected,
 *         inputProcessor = ::processInput
 *     )
 * )
 * ```
 *
 * @param eventFactory Function that creates an event from the parameter
 * @param inputProcessor Function that processes the event (typically ViewModel::input)
 * @return A callback function that takes a parameter and emits the corresponding event
 */
fun <Param, Event> inputCallbackWithParam(
    eventFactory: (Param) -> Event,
    inputProcessor: (Event) -> Unit
): (Param) -> Unit = { param ->
    inputProcessor(eventFactory(param))
}

/**
 * Extension function for UsfViewModel to create inputCallback more conveniently.
 *
 * Usage:
 * ```
 * override fun initialState() = MyUiState(
 *     onBackClicked = inputCallback(MyEvent.BackClicked)
 * )
 * ```
 */
fun <Event : Any, UiState : Any, Effect : Any> UsfViewModel<Event, UiState, Effect>.inputCallback(
    event: Event
): () -> Unit = inputCallback(event, ::input)

/**
 * Extension function for UsfViewModel to create inputCallbackWithParam more conveniently.
 *
 * Usage:
 * ```
 * override fun initialState() = MyUiState(
 *     onTextChanged = inputCallbackWithParam(MyEvent::TextChanged)
 * )
 * ```
 */
fun <Param, Event : Any, UiState : Any, Effect : Any> UsfViewModel<Event, UiState, Effect>.inputCallbackWithParam(
    eventFactory: (Param) -> Event
): (Param) -> Unit = inputCallbackWithParam(eventFactory, ::input)