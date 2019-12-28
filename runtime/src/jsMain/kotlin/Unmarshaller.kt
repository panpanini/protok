package jp.co.panpanini

import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readBytes
import kotlinx.io.core.readText

actual class Unmarshaller(private val input: IoBuffer, private val discardUnknownFields: Boolean = false) {
    private var currentUnknownFields = if (discardUnknownFields) null else mutableMapOf<Int, UnknownField>()

    companion object {
        fun fromByteArray(array: ByteArray): Unmarshaller {
            return Unmarshaller(IoBuffer.Empty.apply { writeFully(array, 0, array.size) })
        }
    }

    actual fun readTag(): Int {
        return input.readInt()
    }

    actual fun readDouble(): Double {
        return input.readDouble()
    }

    actual fun readFloat(): Float {
        return input.readFloat()
    }

    actual fun readInt32(): Int {
        return input.readInt()
    }

    actual fun readInt64(): Long {
        return input.readLong()
    }

    actual fun readUInt32(): Int {
        return input.readInt()
    }

    actual fun readUInt64(): Long {
        return input.readLong()
    }

    actual fun readSInt32(): Int {
        return input.readInt()
    }

    actual fun readSInt64(): Long {
        return input.readLong()
    }

    actual fun readFixed32(): Int {
        return input.readInt()
    }

    actual fun readFixed64(): Long {
        return input.readLong()
    }

    actual fun readSFixed32(): Int {
        return input.readInt()
    }

    actual fun readSFixed64(): Long {
        return input.readLong()
    }

    actual fun readBool(): Boolean {
        return input.readByte() == 1.toByte()
    }

    actual fun readString(): String {
        return input.readText()
    }

    actual fun readBytes(): ByteArr {
        return ByteArr(input.readBytes())
    }

    actual fun <T : Message.Enum> readEnum(companion: Message.Enum.Companion<T>): T {
        return companion.fromValue(input.readInt())
    }

    actual fun <T : Message<T>> readMessage(companion: Message.Companion<T>): T {
        val messageLimit = input.readInt()
        val buffer = input.readBytes(messageLimit)
        return companion.protoUnmarshal(fromByteArray(buffer))
    }

    actual fun <T> readRepeated(appendTo: List<T>?, neverPacked: Boolean, readFunction: () -> T): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun <T : Message.Enum> readRepeatedEnum(appendTo: List<T>?, companion: Message.Enum.Companion<T>): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun <T : Message<T>> readRepeatedMessage(appendTo: List<T>?, companion: Message.Companion<T>, neverPacked: Boolean): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun <K, V, T : Message<T>> readMap(appendTo: Map<K, V>?, companion: Message.Companion<T>, neverPacked: Boolean): Map<K, V> where T : Map.Entry<K, V> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual fun unknownField() {
    }

    actual fun unknownFields(): Map<Int, UnknownField> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}