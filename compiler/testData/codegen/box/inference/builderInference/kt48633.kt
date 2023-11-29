// DONT_TARGET_EXACT_BACKEND: WASM
// WITH_STDLIB
// IGNORE_BACKEND_K2: NATIVE

class TowerDataElementsForName() {
    @OptIn(ExperimentalStdlibApi::class)
    val reversedFilteredLocalScopes = buildList {
        class Foo {
            val reversedFilteredLocalScopes = {
                add("OK")
            }
        }
        Foo().reversedFilteredLocalScopes()
    }
}

fun box(): String {
    return TowerDataElementsForName().reversedFilteredLocalScopes[0]
}