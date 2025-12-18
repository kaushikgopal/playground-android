package sh.kau.playground.usf.inspector

/**
 * A no-operation implementation of [UsfInspector] that performs no actions.
 *
 * Since [UsfInspector] now provides default implementations for all methods, this object simply
 * inherits those defaults. It exists primarily for explicit intent and backward compatibility.
 */
object NoOpInspector : UsfInspector
