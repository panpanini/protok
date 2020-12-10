package jp.co.panpanini


class Unmarshaller(private val reader: Reader, private val discardUnknownFields: Boolean = false) {
    private var currentUnknownFields = if (discardUnknownFields) null else mutableMapOf<Int, UnknownField>()

    companion object {
        fun fromByteArray(array: ByteArray): Unmarshaller {
            return Unmarshaller(Reader(array))
        }
    }

    fun readTag() = reader.readTag()

    fun readDouble() = reader.readDouble()

    fun readFloat() = reader.readFloat()

    fun readInt32() = reader.readInt32()

    fun readInt64() = reader.readInt64()

    fun readUInt32() = reader.readUInt32()

    fun readUInt64() = reader.readUInt64()

    fun readSInt32() = reader.readSInt32()

    fun readSInt64() = reader.readSInt64()

    fun readFixed32() = reader.readFixed32()

    fun readFixed64() = reader.readFixed64()

    fun readSFixed32() = reader.readSFixed32()

    fun readSFixed64() = reader.readSFixed64()

    fun readBool() = reader.readBool()

    fun readString() = reader.readString()

    fun readBytes() = ByteArr(reader.readByteArray())

    fun <T: Message.Enum> readEnum(companion: Message.Enum.Companion<T>): T {
        return companion.fromValue(reader.readEnum())
    }

    fun <T: Message<T>> readMessage(companion: Message.Companion<T>): T {
        val previousLimit = reader.pushLimit(reader.readRawVarint32())
        val unknownFields = currentUnknownFields
        if (!discardUnknownFields) {
            currentUnknownFields = mutableMapOf()
        }
        val message = companion.protoUnmarshal(this)
        require(reader.isAtEnd) {
            "Unable to completely read stream for message ${message::class.java}"
        }
        reader.popLimit(previousLimit)
        currentUnknownFields = unknownFields
        return message
    }

    fun <T> readRepeated(appendTo: List<T>?, neverPacked: Boolean, readFunction: () -> T): List<T> {
        val list = appendTo?.toMutableList() ?: mutableListOf()
        // If not length delimited, then we just have a single item
        if (neverPacked || WireFormat.getTagWireType(reader.lastTag) != WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            list.add(readFunction())
        } else {
            val length = reader.readRawVarint32()
            val oldLimit = reader.pushLimit(length)
            while (!reader.isAtEnd) {
                list.add(readFunction())
            }
            reader.popLimit(oldLimit)
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
        if (neverPacked || WireFormat.getTagWireType(reader.lastTag) != WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            val message = readMessage(companion)
            map[message.key] = message.value
        } else {
            val length = reader.readRawVarint32()
            val oldLimit = reader.pushLimit(length)
            while (!reader.isAtEnd) {
                val (key, value) = readMessage(companion)
                map[key] = value
            }
            reader.popLimit(oldLimit)
        }

        return map
    }

    fun unknownField() {
        val tag = reader.lastTag
        val unknownFields = currentUnknownFields ?: return run { reader.skipField(tag) }
        val value = when (WireFormat.getTagWireType(tag)) {
            WireFormat.WIRETYPE_VARINT -> UnknownField.Value.Varint(reader.readInt64())
            WireFormat.WIRETYPE_FIXED64 -> UnknownField.Value.Fixed64(reader.readFixed64())
            WireFormat.WIRETYPE_LENGTH_DELIMITED -> UnknownField.Value.LengthDelimited(ByteArr(reader.readByteArray()))
            WireFormat.WIRETYPE_START_GROUP -> UnknownField.Value.StartGroup
            WireFormat.WIRETYPE_END_GROUP -> UnknownField.Value.EndGroup
            WireFormat.WIRETYPE_FIXED32 -> UnknownField.Value.Fixed32(reader.readFixed32())
            else -> error("Unrecognized wire type")
        }
        unknownFields.computeLocal(WireFormat.getTagFieldNumber(tag)) { fieldNum, prevVal ->
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

    private fun <K, V> MutableMap<K, V>.computeLocal(key: K, remappingFunction: (K, V?) -> V): V? {
        val oldValue = get(key)

        val newValue = remappingFunction(key, oldValue)
        return if (newValue == null) {
            // delete mapping
            if (oldValue != null || containsKey(key)) {
                // something to remove
                remove(key)
                null
            } else {
                // nothing to do. Leave things as they were.
                null
            }
        } else {
            // add or replace old mapping
            put(key, newValue)
            newValue
        }
    }
}