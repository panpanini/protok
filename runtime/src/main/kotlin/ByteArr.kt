package jp.co.panpanini

import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.util.Base64

class ByteArr(val array: ByteArray = ByteArray(0)) : Serializable {
    override fun equals(other: Any?) = other is ByteArr && array.contentEquals(other.array)
    override fun hashCode() = array.contentHashCode()
    override fun toString() = array.contentToString()
    fun base64Encode(): String {
        return String(Base64.getUrlEncoder().encode(array), StandardCharsets.UTF_8)
    }
    companion object {
        fun base64Decode(base64String: String): ByteArr {
            return ByteArr(Base64.getUrlDecoder().decode(base64String))
        }
    }
}