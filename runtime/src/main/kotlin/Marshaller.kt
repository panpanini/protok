package jp.co.panpanini

import com.google.protobuf.CodedOutputStream

class Marshaller(private val stream: CodedOutputStream, private val bytes: ByteArray? = null) {

    companion object {
        fun allocate(size: Int) = ByteArray(size).let { Marshaller(CodedOutputStream.newInstance(it), it) }
    }

    fun writeTag(tag: Int) = this.apply { stream.writeInt32NoTag(tag) }

    fun writeTag(fieldNum: Int, wireType: Int) = this.apply { stream.writeInt32NoTag((fieldNum shl 3) or wireType) }

    fun writeDouble(value: Double) = this.apply { stream.writeDoubleNoTag(value) }

    fun writeFloat(value: Float) = this.apply { stream.writeFloatNoTag(value) }

    fun writeInt32(value: Int) = this.apply { stream.writeInt32NoTag(value) }

    fun writeInt64(value: Long) = this.apply { stream.writeInt64NoTag(value) }

    fun writeUInt32(value: Int) = this.apply { stream.writeUInt32NoTag(value) }

    fun writeUInt64(value: Long) = this.apply { stream.writeUInt64NoTag(value) }

    fun writeSInt32(value: Int) = this.apply { stream.writeSInt32NoTag(value) }

    fun writeSInt64(value: Long) = this.apply { stream.writeSInt64NoTag(value) }

    fun writeFixed32(value: Int) = this.apply { stream.writeFixed32NoTag(value) }

    fun writeFixed64(value: Long) = this.apply { stream.writeFixed64NoTag(value) }

    fun writeSFixed32(value: Int) = this.apply { stream.writeSFixed32NoTag(value) }

    fun writeSFixed64(value: Long) = this.apply { stream.writeSFixed64NoTag(value) }

    fun writeBool(value: Boolean) = this.apply { stream.writeBoolNoTag(value) }

    fun writeString(value: String) = this.apply { stream.writeStringNoTag(value) }

    fun writeBytes(value: ByteArr) = this.apply { writeBytes(value.array) }

    fun writeBytes(value: ByteArray) = this.apply { stream.writeByteArrayNoTag(value) }

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

    fun complete() = bytes

    fun <K, V, T : Message<T>> writeMap(
            tag: Int,
            map: Map<K, V>,
            createEntry: (K, V) -> T
    ) = this.apply {
        map.entries.forEach {
            writeTag(tag).writeMessage(it as? Message<*> ?: createEntry(it.key, it.value))
        }
    }

}