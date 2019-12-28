package jp.co.panpanini

actual class ByteArr(private val arr: ByteArray = ByteArray(0)) {
    actual val byteArray: ByteArray
        get() = arr

    override fun equals(other: Any?) = other is ByteArr && byteArray.contentEquals(other.byteArray)
    override fun hashCode() = byteArray.contentHashCode()
    override fun toString() = byteArray.contentToString()
}
