package jp.co.panpanini

import java.io.Serializable

actual class ByteArr(actual val byteArray: ByteArray = ByteArray(0)) : Serializable {
    override fun equals(other: Any?) = other is ByteArr && byteArray.contentEquals(other.byteArray)
    override fun hashCode() = byteArray.contentHashCode()
    override fun toString() = byteArray.contentToString()
}