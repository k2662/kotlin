// ISSUE: KT-52838

open class Buildee<A, B> {
    fun materializeA(): A = null as A
    fun materializeB(): B = null as B
}

class DerivedBuildee<A, B>: Buildee<A, B>()

fun interface Builder<A, B> {
    fun build(arg: Buildee<A, B>)
}

fun box(): String {
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    Builder {
        it as DerivedBuildee<*, *>
        it.materializeA()
        it.materializeB() // K1: RECEIVER_TYPE_MISMATCH (expected CapturedType(*), actual CapturedType(*))
    }
    return "OK"
}
