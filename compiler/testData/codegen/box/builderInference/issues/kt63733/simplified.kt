// ISSUE: KT-63733

class Buildee<T: TargetTypeBase> {
    fun yield(arg: T) {} // [1]
}

fun Buildee<TargetType>.yield(arg: DifferentType) {} // [2]

fun <T: TargetTypeBase> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

open class TargetTypeBase
class TargetType: TargetTypeBase()
class DifferentType

fun box(): String {
    // K2: NEW_INFERENCE_ERROR (at Incorporate kotlin/Any <: TypeVariable(T) from For builder inference call from position For builder inference call: kotlin/Any <!: TargetTypeBase)
    build {
        yield(TargetType())
        // K1: (resolves to [1]) TYPE_MISMATCH (expected TargetTypeBase, actual DifferentType)
        // K2: (resolves to [1])
        yield(DifferentType())
    }
    build<TargetType> {
        yield(TargetType())
        // K1 & K2: (resolves to [2]) ok
        yield(DifferentType())
    }
    return "OK"
}
