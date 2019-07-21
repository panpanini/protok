package jp.co.panpanini

import com.google.protobuf.CodedOutputStream
import pbandk.ByteArr

object Sizer {
    fun tagSize(fieldNum: Int): Int {
        return uInt32Size(fieldNum shl 3)
    }

    fun int32Size(value: Int): Int {
        return if (value >= 0) uInt32Size(value) else 10
    }

    fun uInt32Size(value: Int): Int {
        return when {
            value and (0.inv() shl 7) == 0 -> 1
            value and (0.inv() shl 14) == 0 -> 2
            value and (0.inv() shl 21) == 0 -> 3
            value and (0.inv() shl 28) == 0 -> 4
            else -> 5
        }
    }

    fun enumSize(value: Enum<*>): Int {
        return int32Size(value.ordinal)
    }

    fun messageSize(value: Message<*>) : Int {
        return uInt32Size(value.protoSize) + value.protoSize
    }

    fun <T> packedRepeatedSize(list: List<T>, sizingFunction: (T) -> Int): Int {
        val protoSize = list.sumBy(sizingFunction)
        return uInt32Size(protoSize) + protoSize
    }

    fun <K, V, T : Message<T>> mapSize(
            fieldNumber: Int,
            map: Map<K, V>,
            createEntry: (K, V) -> T
    ) = tagSize(fieldNumber).let { tagSize ->
        map.entries.sumBy { entry ->
            val message = entry as? Message<*> ?: createEntry(entry.key, entry.value)

            tagSize + uInt32Size(message.protoSize) + message.protoSize
        }
    }

    fun int64Size(value: Long) = uInt64Size(value)

    fun uInt64Size(value: Long) = CodedOutputStream.computeUInt64SizeNoTag(value)

    fun bytesSize(value: ByteArray) = uInt32Size(value.size) + value.size

    fun bytesSize(value: ByteArr) = bytesSize(value.array)

    fun sInt32Size(value: Int) = uInt32Size(value.zigZagEncode())

    fun sInt64Size(value: Long) = uInt64Size(value.zigZagEncode())

    fun doubleSize(value: Double) = 8

    fun floatSize(value: Float) = 4

    fun fixed32Size(value: Int) = 4

    fun fixed64Size(value: Long) = 8

    fun sFixed32Size(value: Int) = 4

    fun sFixed64Size(value: Long) = 8

    fun boolSize(value: Boolean) = 1

    fun stringSize(value: String) = CodedOutputStream.computeStringSizeNoTag(value)

    private fun Int.zigZagEncode() = (this shl 1) xor (this shr 31)
    private fun Long.zigZagEncode() = (this shl 1) xor (this shr 63)
}