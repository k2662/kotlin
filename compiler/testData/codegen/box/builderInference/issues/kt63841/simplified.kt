// ISSUE: KT-63841
// IGNORE_BACKEND_K1: ANY

fun box(): String {
    // K2: TypeVariable(PTV) => TargetType
    val targetTypeBuildee = build {
        var temp = materialize()
        temp = TargetType()
        temp.foo() // K2: ok
    }
    targetTypeBuildee.materialize().foo() // K2: ok
    targetTypeBuildee.materialize().bar() // K2: UNRESOLVED_REFERENCE

    // K2: TypeVariable(PTV) => DifferentType
    val differentTypeBuildee = build {
        var temp = materialize()
        temp = DifferentType()
        temp.foo() // K2: UNRESOLVED_REFERENCE
    }
    differentTypeBuildee.materialize().foo() // K2: UNRESOLVED_REFERENCE
    differentTypeBuildee.materialize().bar() // K2: ok

    // K2: TypeVariable(PTV) => Any
    val anyBuildee = build {
        var temp = materialize()
        temp = TargetType()
        temp.foo() // K2: ok
        temp = DifferentType()
        temp.foo() // K2: UNRESOLVED_REFERENCE
        temp = TargetType()
        temp.foo() // K2: ok
    }
    anyBuildee.materialize().foo() // K2: UNRESOLVED_REFERENCE
    anyBuildee.materialize().bar() // K2: UNRESOLVED_REFERENCE

    return "OK"
}

class Buildee<TV> {
    fun materialize(): TV = TargetType() as TV
}

fun <PTV> build(
    instructions: Buildee<PTV>.() -> Unit
): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}

class TargetType { fun foo() {} }
class DifferentType { fun bar() {} }
