// ISSUE: KT-63840

fun box(): String {
    build {
        when {
            // K1: TYPE_MISMATCH (expected DifferentType, actual TargetType)
            // K2/JVM: run-time failure (java.lang.ClassCastException: class TargetType cannot be cast to class DifferentType)
            true -> identity(TargetType())
            else -> DifferentType()
        }
    }
    return "OK"
}

class Buildee<TV> {
    fun identity(arg: TV): TV = arg
}

fun <PTV> build(
    instructions: Buildee<PTV>.() -> Unit
): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}

class TargetType
class DifferentType
