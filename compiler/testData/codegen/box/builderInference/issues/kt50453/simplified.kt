// ISSUE: KT-50453

class Buildee<T: Any> {
    private lateinit var data: T
    fun yield(arg: T) { data = arg }
    fun materialize(): T = data
}

fun <T: Any> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType
class DifferentType

fun consume(x: Any) {}
fun consume(x: DifferentType) {}

fun box(): String {
    build {
        yield(TargetType())
        // K1: TYPE_MISMATCH (expected DifferentType, actual TargetType)
        // K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
        consume(materialize())
    }
    return "OK"
}
