package jp.co.panpanini

import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeText

actual class Marshaller(private val output: IoBuffer = IoBuffer.Empty) {


    actual fun writeTag(tag: Int) = this.apply { output.writeInt(tag) }

    actual fun writeTag(fieldNum: Int, wireType: Int) = this.apply { output.writeInt((fieldNum shl 3) or wireType) }

    actual fun writeDouble(value: Double) {
        output.writeDouble(value)
    }

    actual fun writeFloat(value: Float) {
        output.writeFloat(value)
    }

    actual fun writeInt32(value: Int) {
        output.writeInt(value)
    }

    actual fun writeInt64(value: Long) {
        output.writeLong(value)
    }

    actual fun writeUInt32(value: Int) {
        output.writeInt(value)
    }

    actual fun writeUInt64(value: Long) {
        output.writeLong(value)
    }

    actual fun writeSInt32(value: Int) {
        output.writeInt(value)
    }

    actual fun writeSInt64(value: Long) {
        output.writeLong(value)
    }

    actual fun writeFixed32(value: Int) {
        output.writeInt(value)
    }

    actual fun writeFixed64(value: Long) {
        output.writeLong(value)
    }

    actual fun writeSFixed32(value: Int) {
        output.writeInt(value)
    }

    actual fun writeSFixed64(value: Long) {
        output.writeLong(value)
    }

    actual fun writeBool(value: Boolean) {
        output.writeByte(if (value) 1 else 0)
    }

    actual fun writeString(value: String) {
        output.writeText(value)
    }

    actual fun writeBytes(value: ByteArr) {
        writeBytes(value.byteArray)
    }

    actual fun writeBytes(value: ByteArray) {
        output.writeFully(value, 0, value.size)
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
        writeInt32(value.protoSize)
        value.protoMarshal(this)
    }

    actual fun writeEnum(value: Message.Enum) {
        writeInt32(value.value)
    }

    actual fun complete(): ByteArray? = output.readBytes()

    actual fun <K, V, T : Message<T>> writeMap(
            tag: Int,
            map: Map<K, V>,
            createEntry: (K, V) -> T
    ): Marshaller {
        map.entries.forEach {
            writeTag(tag)
            writeMessage(it as? Message<*> ?: createEntry(it.key, it.value))
        }
        return this
    }

    actual fun <T> writePackedRepeated(list: List<T>, sizeFunction: (T) -> Int, writeFunction: (T) -> Unit) {
        writeUInt32(list.sumBy(sizeFunction))
        list.forEach(writeFunction)
    }

}