// WITH_STDLIB

object Property {
    const val PROPERTY_VALUE: String = "O"
}

const val TOP_LEVEL_PROPERTY_VALUE: String = "K"

val value1: String by Property::PROPERTY_VALUE
val value2: String by ::TOP_LEVEL_PROPERTY_VALUE

fun box(): String {
    return value1 + value2
}
