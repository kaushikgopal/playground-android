package sh.kau.playground.usf.inspector

import sh.kau.playground.usf.api.UsfLogger


// TODO: move this to the logger module
//
///**
// * Implementation of [UsfInspector] that logs USF component lifecycle events.
// *
// * This inspector delegates to a [UsfVmSubLogger] to perform the actual logging operations,
// * providing visibility into the flow of events, results, state updates, and effects through the USF
// * pipeline.
// *
// * @param logger The logger implementation to use for logging events. Defaults to
// *   [DefaultViewModelSubLogger].
// */
//class LoggingInspector(private val logger: UsfLogger = DefaultUsfLogger) :
//  UsfInspector {
//
//  override fun onPipelineStarted() {
//    logger.verbose("[ev →  s|ef] ▶")
//  }
//
//  override fun onPipelineStopped() {
//    logger.verbose("[ev →  s|ef] ⏹")
//  }
//
//  override fun onEvent(event: Any) {
//    logger.logEvent(event)
//  }
//
//  override fun onResult(result: Any) {
//    logger.logResults(result)
//  }
//
//  override fun onStateUpdated(state: Any) {
//    logger.logViewState(state)
//  }
//
//  override fun onEffect(effect: Any) {
//    logger.logEffect(effect)
//  }
//
//  override fun error(error: Throwable, message: String) {
//    logger.logError(error, message)
//  }
//
//  override fun debug(message: String) {
//    logger.debug(message)
//  }
//}
