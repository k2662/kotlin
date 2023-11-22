// ISSUE: KT-47982
// WITH_STDLIB

fun box(): String {
    // K1: exception while analyzing expression (java.util.ConcurrentModificationException @ org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession$Companion.updateCalls)
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    val x = buildMap {
        // K2: UNSUPPORTED (collection literals outside of annotations)
        val x = put("", this[[]])
    }
    return "OK"
}
