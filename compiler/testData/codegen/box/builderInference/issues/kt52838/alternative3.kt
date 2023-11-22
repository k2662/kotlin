// ISSUE: KT-52838

open class Buildee<T> {
    fun materialize(): T = null as T
}

class DerivedBuildee<T>: Buildee<T>()

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return DerivedBuildee<T>().apply(block)
}

fun box(): String {
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        this as DerivedBuildee<*>
        materialize()
    }
    return "OK"
}
