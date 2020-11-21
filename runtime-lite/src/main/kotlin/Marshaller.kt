package jp.co.panpanini

class Marshaller(private val writer: Writer) {

    companion object {
        fun allocate(size: Int) = Marshaller(Writer.allocate(size))
    }

    fun writeTag(tag: Int) = this.apply { writer.writeTag(tag) }

    fun writeTag(fieldNum: Int, wireType: Int) = this.apply { writer.writeTag((fieldNum shl 3) or wireType) }

    fun writeDouble(value: Double) {
        writer.writeDouble(value)
    }

    fun writeFloat(value: Float) {
        writer.writeFloat(value)
    }

    fun writeInt32(value: Int) {
        writer.writeInt32(value)
    }

    fun writeInt64(value: Long) {
        writer.writeInt64(value)
    }

    fun writeUInt32(value: Int) {
        writer.writeUInt32(value)
    }

    fun writeUInt64(value: Long) {
        writer.writeUInt64(value)
    }

    fun writeSInt32(value: Int) {
        writer.writeSInt32(value)
    }

    fun writeSInt64(value: Long) {
        writer.writeSInt64(value)
    }

    fun writeFixed32(value: Int) {
        writer.writeFixed32(value)
    }

    fun writeFixed64(value: Long) {
        writer.writeFixed64(value)
    }

    fun writeSFixed32(value: Int) {
        writer.writeSFixed32(value)
    }

    fun writeSFixed64(value: Long) {
        writer.writeSFixed64(value)
    }

    fun writeBool(value: Boolean) {
        writer.writeBool(value)
    }

    fun writeString(value: String) {
        writer.writeString(value)
    }

    fun writeBytes(value: ByteArr) {
        writeBytes(value.array)
    }

    fun writeBytes(value: ByteArray) {
        writer.writeBytes(value)
    }

    fun writeUnknownFields(fields: Map<Int, UnknownField>) {
        fun writeUnknownFieldValue(fieldNum: Int, v: UnknownField.Value) {
            when (v) {
                is UnknownField.Value.Varint -> writeTag(fieldNum, 0).writeUInt64(v.varint)
                is UnknownField.Value.Fixed64 -> writeTag(fieldNum, 1).writeFixed64(v.fixed64)
                is UnknownField.Value.LengthDelimited -> writeTag(fieldNum, 2).writeBytes(v.bytes)
                is UnknownField.Value.StartGroup -> TODO()
                is UnknownField.Value.EndGroup -> TODO()
                is UnknownField.Value.Fixed32 -> writeTag(fieldNum, 5).writeFixed32(v.fixed32)
                is UnknownField.Value.Composite -> v.values.forEach { writeUnknownFieldValue(fieldNum, it) }
            }
        }
        fields.forEach { writeUnknownFieldValue(it.key, it.value.value) }
    }

    fun writeMessage(value: Message<*>) = this.apply {
        writeUInt32(value.protoSize)
        value.protoMarshal(this)
    }

    fun writeEnum(value: Message.Enum) {
        writeInt32(value.value)
    }

    fun complete() = writer.complete()

    fun <K, V, T : Message<T>> writeMap(
            tag: Int,
            map: Map<K, V>,
            createEntry: (K, V) -> T
    ) = this.apply {
        map.entries.forEach {
            writeTag(tag).writeMessage(it as? Message<*> ?: createEntry(it.key, it.value))
        }
    }

    fun <T> writePackedRepeated(list: List<T>, sizeFunction: (T) -> Int, writeFunction: (T) -> Unit) {
        writeUInt32(list.sumBy(sizeFunction))
        list.forEach(writeFunction)
    }

}