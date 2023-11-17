// IGNORE_BACKEND_K2: JVM_IR

fun box() = verifyMessage(
    """
    Assertion failed
    assert("Hello, World".matches("[A-Za-z]+".toRegex()))
                          |                   |
                          |                   [A-Za-z]+
                          false
    """.trimIndent()
) {
    assert("Hello, World".matches("[A-Za-z]+".toRegex()))
}
