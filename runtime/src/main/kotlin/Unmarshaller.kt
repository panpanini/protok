package jp.co.panpanini

import com.google.protobuf.CodedInputStream
import com.google.protobuf.WireFormat
import pbandk.ByteArr

class Unmarshaller(private val stream: CodedInputStream, private val discardUnknownFields: Boolean = false) {
    private var currentUnknownFields = if (discardUnknownFields) null else mutableMapOf<Int, UnknownField>()

    companion object {
        fun fromByteArray(array: ByteArray): Unmarshaller {
            return Unmarshaller(CodedInputStream.newInstance(array))
        }
    }


    fun readTag() = stream.readTag()

    fun readDouble() = stream.readDouble()

    fun readFloat() = stream.readFloat()

    fun readInt32() = stream.readInt32()

    fun readInt64() = stream.readInt64()

    fun readUInt32() = stream.readUInt32()

    fun readUInt64() = stream.readUInt64()

    fun readSInt32() = stream.readSInt32()

    fun readSInt64() = stream.readSInt64()

    fun readFixed32() = stream.readFixed32()

    fun readFixed64() = stream.readFixed64()

    fun readSFixed32() = stream.readSFixed32()

    fun readSFixed64() = stream.readSFixed64()

    fun readBool() = stream.readBool()

    fun readString() = stream.readString()

    fun readBytes() = ByteArr(stream.readByteArray())

    fun <T: Message.Enum> readEnum(companion: Message.Enum.Companion<T>): T {
        return companion.fromValue(stream.readEnum())
    }

    fun <T: Message<T>> readMessage(companion: Message.Companion<T>): T {
        val previousLimit = stream.pushLimit(stream.readRawVarint32())
        val unknownFields = currentUnknownFields
        if (!discardUnknownFields) {
            currentUnknownFields = mutableMapOf()
        }
        val message = companion.protoUnmarshal(this)
        require(stream.isAtEnd) {
            "Unable to completely read stream for message ${message::class.java}"
        }
        stream.popLimit(previousLimit)
        currentUnknownFields = unknownFields
        return message
    }

    fun <T> readRepeated(appendTo: List<T>?, neverPacked: Boolean, readFunction: () -> T): List<T> {
        val list = appendTo?.toMutableList() ?: mutableListOf()
        // If not length delimited, then we just have a single item
        if (neverPacked || WireFormat.getTagWireType(stream.lastTag) != WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            list.add(readFunction())
        } else {
            val length = stream.readRawVarint32()
            val oldLimit = stream.pushLimit(length)
            while (!stream.isAtEnd) {
                list.add(readFunction())
            }
            stream.popLimit(oldLimit)
        }
        return list
    }

    fun <T: Message.Enum> readRepeatedEnum(
            appendTo: List<T>?,
            companion: Message.Enum.Companion<T>
    ): List<T> {
        return readRepeated(appendTo, false) {
            readEnum(companion)
        }
    }

    fun <T: Message<T>> readRepeatedMessage(
            appendTo: List<T>?,
            companion: Message.Companion<T>,
            neverPacked: Boolean
    ): List<T> {
        return readRepeated(appendTo, neverPacked) {
            readMessage(companion)
        }
    }

    fun <K, V, T> readMap(appendTo: Map<K, V>?, companion: Message.Companion<T>, neverPacked: Boolean): Map<K, V> where T : Message<T>, T: Map.Entry<K, V> {
        val map = appendTo?.toMutableMap() ?: mutableMapOf()
        // If not length delimited, then we just have a single item
        if (neverPacked || WireFormat.getTagWireType(stream.lastTag) != WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            val message = readMessage(companion)
            map[message.key] = message.value
        } else {
            val length = stream.readRawVarint32()
            val oldLimit = stream.pushLimit(length)
            while (!stream.isAtEnd) {
                val (key, value) = readMessage(companion)
                map[key] = value
            }
            stream.popLimit(oldLimit)
        }

        return map
    }

    fun unknownField() {
        val tag = stream.lastTag
        val unknownFields = currentUnknownFields ?: return run { stream.skipField(tag) }
        val value = when (WireFormat.getTagWireType(tag)) {
            WireFormat.WIRETYPE_VARINT -> UnknownField.Value.Varint(stream.readInt64())
            WireFormat.WIRETYPE_FIXED64 -> UnknownField.Value.Fixed64(stream.readFixed64())
            WireFormat.WIRETYPE_LENGTH_DELIMITED -> UnknownField.Value.LengthDelimited(ByteArr(stream.readByteArray()))
            WireFormat.WIRETYPE_START_GROUP -> UnknownField.Value.StartGroup
            WireFormat.WIRETYPE_END_GROUP -> UnknownField.Value.EndGroup
            WireFormat.WIRETYPE_FIXED32 -> UnknownField.Value.Fixed32(stream.readFixed32())
            else -> error("Unrecognized wire type")
        }
        unknownFields.compute(WireFormat.getTagFieldNumber(tag)) { fieldNum, prevVal ->
            UnknownField(fieldNum, prevVal?.value.let {
                when (it) {
                    null -> value
                    is UnknownField.Value.Composite -> it.copy(values = it.values + value)
                    else -> UnknownField.Value.Composite(listOf(it, value))
                }
            })
        }
    }

    fun unknownFields(): Map<Int, UnknownField> = currentUnknownFields ?: emptyMap()

}