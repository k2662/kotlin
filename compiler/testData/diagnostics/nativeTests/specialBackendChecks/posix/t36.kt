// FIR_IDENTICAL
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun foo() = stat(malloc(42u)!!.rawValue)
