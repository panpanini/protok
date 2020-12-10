package jp.co.panpanini

import java.io.Serializable

class ByteArr(val array: ByteArray = ByteArray(0)) : Serializable {
    override fun equals(other: Any?) = other is ByteArr && array.contentEquals(other.array)
    override fun hashCode() = array.contentHashCode()
    override fun toString() = array.contentToString()
}