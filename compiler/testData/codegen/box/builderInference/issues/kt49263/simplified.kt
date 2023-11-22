// ISSUE: KT-49263

fun <T> build(block: T.() -> Unit): T {
    return (TargetType() as T).apply(block)
}

class TargetType
fun consume(arg: TargetType) {}

fun box(): String {
    // K1&K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        // K2: CANNOT_INFER_PARAMETER_TYPE
        consume(this)
    }
    return "OK"
}
