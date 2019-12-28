package jp.co.panpanini

expect class ByteArr {
    val byteArray: ByteArray
}

expect class Marshaller {
    fun writeTag(tag: Int): Marshaller
    fun writeTag(fieldNum: Int, wireType: Int): Marshaller

    fun writeDouble(value: Double)

    fun writeFloat(value: Float)

    fun writeInt32(value: Int)

    fun writeInt64(value: Long)

    fun writeUInt32(value: Int)

    fun writeUInt64(value: Long)

    fun writeSInt32(value: Int)

    fun writeSInt64(value: Long)

    fun writeFixed32(value: Int)

    fun writeFixed64(value: Long)

    fun writeSFixed32(value: Int)

    fun writeSFixed64(value: Long)

    fun writeBool(value: Boolean)

    fun writeString(value: String)

    fun writeBytes(value: ByteArr)

    fun writeBytes(value: ByteArray)

    fun writeUnknownFields(fields: Map<Int, UnknownField>)

    fun writeMessage(value: Message<*>): Marshaller

    fun writeEnum(value: Message.Enum)

    fun complete(): ByteArray?

    fun <K, V, T : Message<T>> writeMap(tag: Int, map: Map<K, V>, createEntry: (K, V) -> T): Marshaller

    fun <T> writePackedRepeated(list: List<T>, sizeFunction: (T) -> Int, writeFunction: (T) -> Unit)

}

expect class Unmarshaller {
    fun readTag(): Int

    fun readDouble(): Double

    fun readFloat(): Float

    fun readInt32(): Int

    fun readInt64(): Long

    fun readUInt32(): Int

    fun readUInt64(): Long

    fun readSInt32(): Int

    fun readSInt64(): Long

    fun readFixed32(): Int

    fun readFixed64(): Long

    fun readSFixed32(): Int

    fun readSFixed64(): Long

    fun readBool(): Boolean

    fun readString(): String

    fun readBytes(): ByteArr

    fun <T: Message.Enum> readEnum(companion: Message.Enum.Companion<T>): T

    fun <T: Message<T>> readMessage(companion: Message.Companion<T>): T

    fun <T> readRepeated(appendTo: List<T>?, neverPacked: Boolean, readFunction: () -> T): List<T>

    fun <T: Message.Enum> readRepeatedEnum(
            appendTo: List<T>?,
            companion: Message.Enum.Companion<T>
    ): List<T>

    fun <T: Message<T>> readRepeatedMessage(
            appendTo: List<T>?,
            companion: Message.Companion<T>,
            neverPacked: Boolean
    ): List<T>

    fun <K, V, T> readMap(appendTo: Map<K, V>?, companion: Message.Companion<T>, neverPacked: Boolean): Map<K, V> where T : Message<T>, T: Map.Entry<K, V>

    fun unknownField()

    fun unknownFields(): Map<Int, UnknownField>

}

expect interface Message<T : Message<T>> {
    fun protoUnmarshal(u: Unmarshaller): T
    fun protoMarshal(m: Marshaller)

    operator fun plus(other: T?): T

    val protoSize: Int

    interface Enum {
        val value: Int

        interface Companion<T : Enum> {
            fun fromValue(value: Int): T
        }
    }

    interface Companion<T: Message<T>> {
        fun protoUnmarshal(u: Unmarshaller): T
    }
}

expect class UnknownField {
    fun size(): Int
    sealed class Value {
        class Varint
        class Fixed64
        class LengthDelimited
        object StartGroup
        object EndGroup
        class Fixed32
        class Composite
    }
}

expect fun computeUInt64Size(value: Long): Int

expect fun computeStringSize(value: String): Int

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

    fun enumSize(value: Message.Enum): Int {
        return int32Size(value.value)
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

    fun uInt64Size(value: Long) = computeUInt64Size(value)

    fun bytesSize(value: ByteArray) = uInt32Size(value.size) + value.size

    fun bytesSize(value: ByteArr) = bytesSize(value.byteArray)

    fun sInt32Size(value: Int) = uInt32Size(value.zigZagEncode())

    fun sInt64Size(value: Long) = uInt64Size(value.zigZagEncode())

    fun doubleSize(value: Double) = 8

    fun floatSize(value: Float) = 4

    fun fixed32Size(value: Int) = 4

    fun fixed64Size(value: Long) = 8

    fun sFixed32Size(value: Int) = 4

    fun sFixed64Size(value: Long) = 8

    fun boolSize(value: Boolean) = 1

    fun stringSize(value: String) = computeStringSize(value)

    private fun Int.zigZagEncode() = (this shl 1) xor (this shr 31)
    private fun Long.zigZagEncode() = (this shl 1) xor (this shr 63)
}
