package jp.co.panpanini

import kotlinx.io.core.toByteArray

actual fun computeUInt64Size(value: Long): Int {
    @Suppress("NAME_SHADOWING")
    var value = value
    // handle two popular special cases up front ...
    if (value and (0L.inv() shl 7) == 0L) {
        return 1
    }
    if (value < 0L) {
        return 10
    }
    // ... leaving us with 8 remaining, which we can divide and conquer
    var n = 2
    if (value and (0L.inv() shl 35) != 0L) {
        n += 4
        value = value ushr 28
    }
    if (value and (0L.inv() shl 21) != 0L) {
        n += 2
        value = value ushr 14
    }
    if (value and (0L.inv() shl 14) != 0L) {
        n += 1
    }
    return n
}

actual fun computeStringSize(value: String): Int {
    return value.toByteArray().size
}

