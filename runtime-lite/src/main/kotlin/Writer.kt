package jp.co.panpanini

class Writer(private val byteArray: ByteArray) {

    private var position = 0

    fun spaceLeft(): Int = byteArray.size - position

    fun writeTag(value: Int) {
        writeUInt32(value)
    }

    fun writeInt32(value: Int) {
        if (value >= 0) {
            writeUInt32(value)
        } else {
            writeUInt64(value.toLong())
        }
    }

    // bytes are written in smallest to largest order
    fun writeUInt32(value: Int) {
        var value = value
        while (true) {
            if (value and 0x7F.inv() == 0) {
                byteArray[position++] = value.toByte()
                return
            } else {
                byteArray[position++] = (value and 0x7F or 0x80).toByte()
                value = value ushr 7
            }
        }
    }

    // bytes are written in smallest to largest order
    fun writeInt64(value: Long) {
        writeUInt64(value)
    }

    // bytes are written in smallest to largest order
    fun writeUInt64(value: Long) {
        var value = value
        while (true) {
            if (value and 0x7FL.inv() == 0L) {
                byteArray[position++] = value.toByte()
                return
            } else {
                byteArray[position++] = (value.toInt() and 0x7F or 0x80).toByte()
                value = value ushr 7
            }
        }
    }

    // bytes are written in smallest to largest order
    fun writeFixed32(value: Int) {
        byteArray[position++] = (value and 0xFF).toByte()
        byteArray[position++] = (value shr 8 and 0xFF).toByte()
        byteArray[position++] = (value shr 16 and 0xFF).toByte()
        byteArray[position++] = (value shr 24 and 0xFF).toByte()
    }

    // bytes are written in smallest to largest order
    fun writeFixed64(value: Long) {
        byteArray[position++] = (value and 0xFF).toByte()
        byteArray[position++] = (value shr 8 and 0xFF).toByte()
        byteArray[position++] = (value shr 16 and 0xFF).toByte()
        byteArray[position++] = (value shr 24 and 0xFF).toByte()
        byteArray[position++] = ((value shr 32).toInt() and 0xFF).toByte()
        byteArray[position++] = ((value shr 40).toInt() and 0xFF).toByte()
        byteArray[position++] = ((value shr 48).toInt() and 0xFF).toByte()
        byteArray[position++] = ((value shr 56).toInt() and 0xFF).toByte()
    }

    fun writeSInt32(value: Int) {
        writeUInt32(value.encodeZigZag32())
    }

    fun writeSInt64(value: Long) {
        writeUInt64(value.encodeZigZag64())
    }

    fun writeSFixed32(value: Int) {
        writeFixed32(value)
    }

    fun writeSFixed64(value: Long) {
        writeFixed64(value)
    }

    fun writeBool(value: Boolean) {
        writeByte((if (value) 1 else 0).toByte())
    }

    fun writeDouble(value: Double) {
        writeFixed64(value.toRawBits())
    }

    fun writeFloat(value: Float) {
        writeFixed32(value.toRawBits())
    }

    fun writeByte(value: Byte) {
        byteArray[position++] = value
    }

    fun writeBytes(value: ByteArray) {
        value.forEach(::writeByte)
    }

    fun writeString(value: String) {
        val oldPosition = position
        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. We take advantage of this in both branches below.
        val maxLength: Int = value.length * Utf8.MAX_BYTES_PER_CHAR
        val maxLengthVarIntSize: Int = computeUInt32Size(maxLength)
        val minLengthVarIntSize: Int = computeUInt32Size(value.length)
        if (minLengthVarIntSize == maxLengthVarIntSize) {
            position = oldPosition + minLengthVarIntSize
            val newPosition: Int = Utf8.encode(value, byteArray, position, spaceLeft())
            // Since this class is stateful and tracks the position, we rewind and store the state,
            // prepend the length, then reset it back to the end of the string.
            position = oldPosition
            val length = newPosition - oldPosition - minLengthVarIntSize
            writeUInt32(length)
            position = newPosition
        } else {
            val length: Int = Utf8.encodedLength(value)
            writeUInt32(length)
            position = Utf8.encode(value, byteArray, position, spaceLeft())
        }
    }

    fun complete(): ByteArray {
        return byteArray
    }

    companion object {
        fun allocate(size: Int) : Writer = Writer(ByteArray(size))
    }
}

private fun computeUInt32Size(value: Int): Int {
    if (value and (0.inv() shl 7) == 0) {
        return 1
    }
    if (value and (0.inv() shl 14) == 0) {
        return 2
    }
    if (value and (0.inv() shl 21) == 0) {
        return 3
    }
    return if (value and (0.inv() shl 28) == 0) {
        4
    } else 5
}

fun Int.encodeZigZag32(): Int {
    // Note:  the right-shift must be arithmetic
    return this shl 1 xor (this shr 31)
}

fun Long.encodeZigZag64(): Long {
    // Note:  the right-shift must be arithmetic
    return this shl 1 xor (this shr 63)
}