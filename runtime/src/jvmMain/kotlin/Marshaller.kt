package jp.co.panpanini

import com.google.protobuf.CodedOutputStream

actual class Marshaller(private val stream: CodedOutputStream, private val bytes: ByteArray? = null) {

    companion object {
        fun allocate(size: Int) = ByteArray(size).let { Marshaller(CodedOutputStream.newInstance(it), it) }
    }

    actual fun writeTag(tag: Int) = this.apply { stream.writeInt32NoTag(tag) }

    actual fun writeTag(fieldNum: Int, wireType: Int) = this.apply { stream.writeInt32NoTag((fieldNum shl 3) or wireType) }

    actual fun writeDouble(value: Double) {
        stream.writeDoubleNoTag(value)
    }

    actual fun writeFloat(value: Float) {
        stream.writeFloatNoTag(value)
    }

    actual fun writeInt32(value: Int) {
        stream.writeInt32NoTag(value)
    }

    actual fun writeInt64(value: Long) {
        stream.writeInt64NoTag(value)
    }

    actual fun writeUInt32(value: Int) {
        stream.writeUInt32NoTag(value)
    }

    actual fun writeUInt64(value: Long) {
        stream.writeUInt64NoTag(value)
    }

    actual fun writeSInt32(value: Int) {
        stream.writeSInt32NoTag(value)
    }

    actual fun writeSInt64(value: Long) {
        stream.writeSInt64NoTag(value)
    }

    actual fun writeFixed32(value: Int) {
        stream.writeFixed32NoTag(value)
    }

    actual fun writeFixed64(value: Long) {
        stream.writeFixed64NoTag(value)
    }

    actual fun writeSFixed32(value: Int) {
        stream.writeSFixed32NoTag(value)
    }

    actual fun writeSFixed64(value: Long) {
        stream.writeSFixed64NoTag(value)
    }

    actual fun writeBool(value: Boolean) {
        stream.writeBoolNoTag(value)
    }

    actual fun writeString(value: String) {
        stream.writeStringNoTag(value)
    }

    actual fun writeBytes(value: ByteArr) {
        writeBytes(value.byteArray)
    }

    actual fun writeBytes(value: ByteArray) {
        stream.writeByteArrayNoTag(value)
    }

    actual fun writeUnknownFields(fields: Map<Int, UnknownField>) {
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

    actual fun writeMessage(value: Message<*>) = this.apply {
        writeUInt32(value.protoSize)
        value.protoMarshal(this)
    }

    actual fun writeEnum(value: Message.Enum) {
        writeInt32(value.value)
    }

    actual fun complete() = bytes

    actual fun <K, V, T : Message<T>> writeMap(
            tag: Int,
            map: Map<K, V>,
            createEntry: (K, V) -> T
    ): Marshaller {
        map.entries.forEach {
            writeTag(tag as Int)
            writeMessage(it as? Message<*> ?: createEntry(it.key, it.value))
        }
        return this
    }

    actual fun <T> writePackedRepeated(list: List<T>, sizeFunction: (T) -> Int, writeFunction: (T) -> Unit) {
        writeUInt32(list.sumBy(sizeFunction))
        list.forEach(writeFunction)
    }

}