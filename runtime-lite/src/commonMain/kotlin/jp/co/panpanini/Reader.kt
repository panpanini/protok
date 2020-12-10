package jp.co.panpanini

import kotlin.experimental.and
import kotlin.math.min

private const val BUFFER_SIZE = 4096
private const val DEFAULT_SIZE_LIMIT = 64 shl 20 // 64MB


class Reader(private val byteArray: ByteArray) {

    /**
     * The total number of bytes read before the current buffer.  The total
     * bytes read up to the current position can be computed as
     * `totalBytesRetired + bufferPos`.  This value may be negative if
     * reading started in the middle of the current buffer (e.g. if the
     * constructor that takes a byte array and an offset was used).
     */
    private var totalBytesRetired = 0

    /** The absolute position of the end of the current message.  */
    private var currentLimit = Int.MAX_VALUE

    /** See setSizeLimit()  */
    private val sizeLimit: Int = DEFAULT_SIZE_LIMIT

    private var bufferSizeAfterLimit = 0

    private var position: Int = 0

    var lastTag: Int = 0

    private var bufferSize = byteArray.size


    /**
     * Returns true if the stream has reached the end of the input.  This is the
     * case if either the end of the underlying input source has been reached or
     * if the stream has reached a limit created using [.pushLimit].
     */
    val isAtEnd: Boolean
            get() = position == bufferSize
    /**
     * Attempt to read a field tag, returning zero if we have reached EOF.
     * Protocol message parsers use this to read tags, since a protocol message
     * may legally end wherever a tag occurs, and zero is not a valid tag number.
     */
    fun readTag(): Int {
        if (isAtEnd) {
            lastTag = 0
            return 0
        }

        lastTag = readRawVarint32()
        if (WireFormat.getTagFieldNumber(lastTag) == 0) {
            // If we actually read zero (or any tag number corresponding to field
            // number zero), that's not a valid tag.
            throw InvalidProtocolBufferException.invalidTag()
        }
        return lastTag
    }

    // -----------------------------------------------------------------
    /** Read a `double` field value from the stream.  */
    fun readDouble(): Double {
        return Double.fromBits(readRawLittleEndian64())
    }

    /** Read a `float` field value from the stream.  */
    fun readFloat(): Float {
        return Float.fromBits(readRawLittleEndian32())
    }

    /** Read a `uint64` field value from the stream.  */
    fun readUInt64(): Long {
        return readRawVarint64()
    }

    /** Read an `int64` field value from the stream.  */
    fun readInt64(): Long {
        return readRawVarint64()
    }

    /** Read an `int32` field value from the stream.  */
    fun readInt32(): Int {
        return readRawVarint32()
    }

    /** Read a `fixed64` field value from the stream.  */
    fun readFixed64(): Long {
        return readRawLittleEndian64()
    }

    /** Read a `fixed32` field value from the stream.  */
    fun readFixed32(): Int {
        return readRawLittleEndian32()
    }

    /** Read a `bool` field value from the stream.  */
    fun readBool(): Boolean {
        return readRawVarint64() != 0L
    }


    /** Read a `uint32` field value from the stream.  */
    fun readUInt32(): Int {
        return readRawVarint32()
    }

    /**
     * Read an enum field value from the stream.  Caller is responsible
     * for converting the numeric value to an actual enum.
     */
    fun readEnum(): Int {
        return readRawVarint32()
    }

    /** Read an `sfixed32` field value from the stream.  */
    fun readSFixed32(): Int {
        return readRawLittleEndian32()
    }

    /** Read an `sfixed64` field value from the stream.  */
    fun readSFixed64(): Long {
        return readRawLittleEndian64()
    }

    /** Read an `sint32` field value from the stream.  */
    fun readSInt32(): Int {
        return decodeZigZag32(readRawVarint32())
    }

    /** Read an `sint64` field value from the stream.  */
    fun readSInt64(): Long {
        return decodeZigZag64(readRawVarint64())
    }

    /**
     * Read a `string` field value from the stream.
     * If the stream contains malformed UTF-8,
     * replace the offending bytes with the standard UTF-8 replacement character.
     */
    fun readString(): String {
        val size = readRawVarint32()
        return if (size <= bufferSize - position && size > 0) {
            // Fast path:  We already have the bytes in a contiguous buffer, so
            //   just copy directly from it.
            val result = byteArray.copyOfRange(position, position + size).decodeToString()
            position += size
            result
        } else if (size == 0) {
            ""
        } else {
            // Slow path:  Build a byte array first then copy it.
            readRawBytesSlowPath(size).decodeToString()
        }
    }


    /** Read a `bytes` field value from the stream.  */
    fun readByteArray(): ByteArray {
        val size = readRawVarint32()
        return if (size <= bufferSize - position && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer, so
            // just copy directly from it.
            val result: ByteArray = byteArray.copyOfRange(position, position + size)
            position += size
            result
        } else {
            // Slow path: Build a byte array first then copy it.
            readRawBytesSlowPath(size)
        }
    }

    /**
     * Sets `currentLimit` to (current position) + `byteLimit`.  This
     * is called when descending into a length-delimited embedded message.
     *
     *
     * Note that `pushLimit()` does NOT affect how many bytes the
     * `Reader` reads from an underlying `ByteArray` when
     * refreshing its buffer.  If you need to prevent reading past a certain
     * point in the underlying `ByteArray` (e.g. because you expect it to
     * contain more data after the end of the message which you need to handle
     * differently) then you must place a wrapper around your `ByteArray`
     * which limits the amount of data that can be read from it.
     *
     * @return the old limit.
     */
    fun pushLimit(byteLimit: Int): Int {
        var byteLimit = byteLimit
        if (byteLimit < 0) {
            throw InvalidProtocolBufferException.negativeSize()
        }
        byteLimit += totalBytesRetired + position
        val oldLimit: Int = currentLimit
        if (byteLimit > oldLimit) {
            throw InvalidProtocolBufferException.truncatedMessage()
        }
        currentLimit = byteLimit
        recomputeBufferSizeAfterLimit()
        return oldLimit
    }

    private fun recomputeBufferSizeAfterLimit() {
        bufferSize += bufferSizeAfterLimit
        val bufferEnd = totalBytesRetired + bufferSize
        if (bufferEnd > currentLimit) {
            // Limit is in current buffer.
            bufferSizeAfterLimit = bufferEnd - currentLimit
            bufferSize -= bufferSizeAfterLimit
        } else {
            bufferSizeAfterLimit = 0
        }
    }

    /**
     * Discards the current limit, returning to the previous limit.
     *
     * @param oldLimit The old limit, as returned by `pushLimit`.
     */
    fun popLimit(oldLimit: Int) {
        currentLimit = oldLimit
        recomputeBufferSizeAfterLimit()
    }

    /**
     * Read a fixed size of bytes from the input.
     *
     * @throws InvalidProtocolBufferException The end of the stream or the current
     * limit was reached.
     */
    fun readRawBytes(size: Int): ByteArray {
        val pos: Int = position
        return if (size <= bufferSize - pos && size > 0) {
            position = pos + size
            byteArray.copyOfRange(pos, pos + size)
        } else {
            readRawBytesSlowPath(size)
        }
    }

    /**
     * Exactly like readRawBytes, but caller must have already checked the fast
     * path: (size <= (bufferSize - pos) && size > 0)
     */
    private fun readRawBytesSlowPath(size: Int): ByteArray {
        if (size <= 0) {
            return if (size == 0) {
                byteArrayOf()
            } else {
                throw InvalidProtocolBufferException.negativeSize()
            }
        }

        // Verify that the message size so far has not exceeded sizeLimit.
        val currentMessageSize: Int = totalBytesRetired + position + size
        if (currentMessageSize > sizeLimit) {
            throw InvalidProtocolBufferException.sizeLimitExceeded()
        }

        // Verify that the message size so far has not exceeded currentLimit.
        if (currentMessageSize > currentLimit) {
            // Read to the end of the stream anyway.
            skipRawBytes(currentLimit - totalBytesRetired - position)
            throw InvalidProtocolBufferException.truncatedMessage()
        }

        val originalBufferPos: Int = position
        val bufferedBytes: Int = bufferSize - position

        // Mark the current buffer consumed.
//        totalBytesRetired += bufferSize
//        position = 0
//        bufferSize = 0

        // Determine the number of bytes we need to read from the input stream.
        var sizeLeft = size - bufferedBytes
        // TODO(nathanmittler): Consider using a value larger than BUFFER_SIZE.
        if (sizeLeft < BUFFER_SIZE) {
            // Either the bytes we need are known to be available, or the required buffer is
            // within an allowed threshold - go ahead and allocate the buffer now.
//            val bytes = ByteArray(size)

            // Copy all of the buffered bytes to the result buffer.
            //            java.lang.System.arraycopy(buffer, originalBufferPos, bytes, 0, bufferedBytes)
            return byteArray.copyOfRange(originalBufferPos, bufferedBytes)
        }

        // The size is very large.  For security reasons, we can't allocate the
        // entire byte array yet.  The size comes directly from the input, so a
        // maliciously-crafted message could provide a bogus very large size in
        // order to trick the app into allocating a lot of memory.  We avoid this
        // by allocating and reading only a small chunk at a time, so that the
        // malicious message must actually *be* extremely large to cause
        // problems.  Meanwhile, we limit the allowed size of a message elsewhere.
        val chunks = mutableListOf<ByteArray>()
        while (sizeLeft > 0) {
            // TODO(nathanmittler): Consider using a value larger than BUFFER_SIZE.
            val chunkSize = min(sizeLeft, BUFFER_SIZE)
            var pos = 0
            var chunk = ByteArray(0)
            while (pos < chunkSize) {
//                val n: Int = input.read(chunk, pos, chunk.size - pos)
                chunk = byteArray.copyOfRange(pos, chunkSize - pos)
                pos += chunkSize
            }
            sizeLeft -= chunkSize
            chunks.add(chunk)
        }

        // OK, got everything.  Now concatenate it all into one buffer.
        val bytes = ByteArray(size)

        // Start by copying the leftover bytes from this.buffer.
//        java.lang.System.arraycopy(buffer, originalBufferPos, bytes, 0, bufferedBytes)
        byteArray.copyInto(bytes, 0, originalBufferPos, bufferedBytes)

        // And now all the chunks.
        var pos = bufferedBytes
        for (chunk in chunks) {
//            arraycopy(chunk, 0, bytes, pos, chunk.size)
            chunk.copyInto(bytes, pos, 0, chunk.size)
            pos += chunk.size
        }

        // Done.
        return bytes
    }

    /**
     * Reads and discards `size` bytes.
     *
     * @throws InvalidProtocolBufferException The end of the stream or the current
     * limit was reached.
     */
    fun skipRawBytes(size: Int) {
        if (size <= bufferSize - position && size >= 0) {
            // We have all the bytes we need already.
            position += size
        } else {
            skipRawBytesSlowPath(size)
        }
    }

    /**
     * Exactly like skipRawBytes, but caller must have already checked the fast
     * path: (size <= (bufferSize - pos) && size >= 0)
     */
    private fun skipRawBytesSlowPath(size: Int) {
        if (size < 0) {
            throw InvalidProtocolBufferException.negativeSize()
        }
        if (totalBytesRetired + position + size > currentLimit) {
            // Read to the end of the stream anyway.
            skipRawBytes(currentLimit - totalBytesRetired - position)
            throw InvalidProtocolBufferException.truncatedMessage()
        }

        //TODO
    }


    // =================================================================
    /**
     * Read a raw Varint from the stream.  If larger than 32 bits, discard the
     * upper bits.
     */
    fun readRawVarint32(): Int {
        // See implementation notes for readRawVarint64
        var pos: Int = position
        if (bufferSize == pos) {
            return readRawVarint64SlowPath().toInt()
        }
        val buffer: ByteArray = this.byteArray
        var x: Int
        if (buffer[pos++].also { x = it.toInt() } >= 0) {
            position = pos
            return x
        } else if (bufferSize - pos < 9) {
            return readRawVarint64SlowPath().toInt()
        } else if ((buffer[pos++].toInt() shl 7).let { x = x xor it; x } < 0) {
            x = x xor (0.inv() shl 7)
        } else if ((buffer[pos++].toInt() shl 14).let { x = x xor it; x } >= 0) {
            x = x xor (0.inv() shl 7 xor (0.inv() shl 14))
        } else if ((buffer[pos++].toInt() shl 21).let { x = x xor it; x } < 0) {
            x = x xor (0.inv() shl 7 xor (0.inv() shl 14) xor (0.inv() shl 21))
        } else {
            val y = buffer[pos++].toInt()
            x = x xor (y shl 28)
            x = x xor (0.inv() shl 7 xor (0.inv() shl 14) xor (0.inv() shl 21) xor (0.inv() shl 28))
            if (y < 0 && buffer[pos++] < 0 && buffer[pos++] < 0 && buffer[pos++] < 0 && buffer[pos++] < 0 && buffer[pos++] < 0) {
                return readRawVarint64SlowPath().toInt() // Will throw malformedVarint()
            }
        }
        position = pos
        return x

    }

    /** Read a raw Varint from the stream.  */
    fun readRawVarint64(): Long {
        // Implementation notes:
        //
        // Optimized for one-byte values, expected to be common.
        // The particular code below was selected from various candidates
        // empirically, by winning VarintBenchmark.
        //
        // Sign extension of (signed) Java bytes is usually a nuisance, but
        // we exploit it here to more easily obtain the sign of bytes read.
        // Instead of cleaning up the sign extension bits by masking eagerly,
        // we delay until we find the final (positive) byte, when we clear all
        // accumulated bits with one xor.  We depend on javac to constant fold.
        var pos: Int = position
        if (bufferSize == pos) {
            return readRawVarint64SlowPath()
        }
        val buffer: ByteArray = this.byteArray
        var x: Long
        var y: Int
        if (buffer[pos++].also { y = it.toInt() } >= 0) {
            position = pos
            return y.toLong()
        } else if (bufferSize - pos < 9) {
            return readRawVarint64SlowPath()
        } else if ((buffer[pos++].toInt() shl 7).let { y = y xor it; y } < 0) {
            x = (y xor (0.inv() shl 7).toLong().toInt()).toLong()
        } else if ((buffer[pos++].toInt() shl 14).let { y = y xor it; y } >= 0) {
            x = (y xor (0.inv() shl 7 xor (0.inv() shl 14)).toLong().toInt()).toLong()
        } else if ((buffer[pos++].toInt() shl 21).let { y = y xor it; y } < 0) {
            x = (y xor (0.inv() shl 7 xor (0.inv() shl 14) xor (0.inv() shl 21)).toLong().toInt()).toLong()
        } else if (y.toLong() xor (buffer[pos++].toLong() shl 28).also { x = it } >= 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28))
        } else if ((buffer[pos++].toLong() shl 35).let { x = x xor it; x } < 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28) xor (0L.inv() shl 35))
        } else if ((buffer[pos++].toLong() shl 42).let { x = x xor it; x } >= 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28) xor (0L.inv() shl 35) xor (0L.inv() shl 42))
        } else if ((buffer[pos++].toLong() shl 49).let { x = x xor it; x } < 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28) xor (0L.inv() shl 35) xor (0L.inv() shl 42)
                    xor (0L.inv() shl 49))
        } else {
            x = x xor (buffer[pos++].toLong() shl 56)
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28) xor (0L.inv() shl 35) xor (0L.inv() shl 42)
                    xor (0L.inv() shl 49) xor (0L.inv() shl 56))
            if (x < 0L) {
                if (buffer[pos++] < 0L) {
                    return readRawVarint64SlowPath()  // Will throw malformedVarint()
                }
            }
        }
        position = pos
        return x
    }

    /** Variant of readRawVarint64 for when uncomfortably close to the limit.  */
    fun readRawVarint64SlowPath(): Long {
        var result: Long = 0
        var shift = 0
        while (shift < 64) {
            val b = readRawByte()
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80.toByte()) == 0.toByte()) {
                return result
            }
            shift += 7
        }
        throw InvalidProtocolBufferException.malformedVarint()
    }

    /** Read a 32-bit little-endian integer from the stream.  */
    fun readRawLittleEndian32(): Int {
        val pos: Int = position

        val buffer: ByteArray = this.byteArray
        position = pos + 4
        return buffer[pos].toInt() and 0xff or
                (buffer[pos + 1].toInt() and 0xff shl 8) or
                (buffer[pos + 2].toInt() and 0xff shl 16) or
                (buffer[pos + 3].toInt() and 0xff shl 24)
    }

    /** Read a 64-bit little-endian integer from the stream.  */
    fun readRawLittleEndian64(): Long {
        val pos: Int = position

        val buffer: ByteArray = this.byteArray
        position = pos + 8
        return buffer[pos].toLong() and 0xffL or
                (buffer[pos + 1].toLong() and 0xffL shl 8) or
                (buffer[pos + 2].toLong() and 0xffL shl 16) or
                (buffer[pos + 3].toLong() and 0xffL shl 24) or
                (buffer[pos + 4].toLong() and 0xffL shl 32) or
                (buffer[pos + 5].toLong() and 0xffL shl 40) or
                (buffer[pos + 6].toLong() and 0xffL shl 48) or
                (buffer[pos + 7].toLong() and 0xffL shl 56)
    }

    fun readRawByte(): Byte {
        if (position == byteArray.size) {
            TODO("throw IllegalStateException here")
        }

        return byteArray[position++]
    }

    /**
     * Reads and discards a single field, given its tag value.
     *
     * @return `false` if the tag is an endgroup tag, in which case
     * nothing is skipped.  Otherwise, returns `true`.
     */
    fun skipField(tag: Int): Boolean {
        return when (WireFormat.getTagWireType(tag)) {
            WireFormat.WIRETYPE_VARINT -> {
                skipRawVarint()
                true
            }
            WireFormat.WIRETYPE_FIXED64 -> {
                skipRawBytes(8)
                true
            }
            WireFormat.WIRETYPE_LENGTH_DELIMITED -> {
                skipRawBytes(readRawVarint32())
                true
            }
            WireFormat.WIRETYPE_START_GROUP -> {
                skipMessage()
                checkLastTagWas(
                        WireFormat.makeTag(
                                WireFormat.getTagFieldNumber(tag),
                                WireFormat.WIRETYPE_END_GROUP
                        )
                )
                true
            }
            WireFormat.WIRETYPE_END_GROUP -> false
            WireFormat.WIRETYPE_FIXED32 -> {
                skipRawBytes(4)
                true
            }
            else -> throw InvalidProtocolBufferException.invalidWireType()
        }
    }

    /**
     * Verifies that the last call to readTag() returned the given tag value.
     * This is used to verify that a nested group ended with the correct
     * end tag.
     *
     * @throws InvalidProtocolBufferException `value` does not match the
     * last tag.
     */
    fun checkLastTagWas(value: Int) {
        if (lastTag != value) {
            throw InvalidProtocolBufferException.invalidEndTag()
        }
    }

    private fun skipRawVarint() {
        if (bufferSize - position >= 10) {
            val buffer: ByteArray = byteArray
            var pos: Int = position
            for (i in 0..9) {
                if (buffer[pos++] >= 0) {
                    position = pos
                    return
                }
            }
        }
        skipRawVarintSlowPath()
    }

    private fun skipRawVarintSlowPath() {
        for (i in 0..9) {
            if (readRawByte() >= 0) {
                return
            }
        }
        throw InvalidProtocolBufferException.malformedVarint()
    }

    /**
     * Reads and discards an entire message.  This will read either until EOF
     * or until an endgroup tag, whichever comes first.
     */
    fun skipMessage() {
        while (true) {
            val tag = readTag()
            if (tag == 0 || !skipField(tag)) {
                return
            }
        }
    }
}

class InvalidProtocolBufferException(message: String) : IllegalStateException(message) {
    companion object {
        fun invalidTag() = InvalidProtocolBufferException("Protocol message contained an invalid tag (zero).")

        fun malformedVarint() = InvalidProtocolBufferException("Reader encountered a malformed varint.")

        fun negativeSize() = InvalidProtocolBufferException(
                "Reader encountered an embedded string or message which claimed to have negative size."
        )

        fun truncatedMessage() = InvalidProtocolBufferException(
                "While parsing a protocol message, the input ended unexpectedly " +
                        "in the middle of a field.  This could mean either that the " +
                        "input has been truncated or that an embedded message " +
                        "misreported its own length."
        )

        fun sizeLimitExceeded() = InvalidProtocolBufferException(
                "Protocol message was too large. May be malicious. Use setSizeLimit() to increase the size limit."
        )

        fun invalidWireType() = InvalidProtocolBufferException("Protocol message tag had invalid wire type.")

        fun invalidEndTag() = InvalidProtocolBufferException(
                "Protocol message end-group tag did not match expected tag."
        )
    }

}

internal object WireFormat {

    const val WIRETYPE_VARINT = 0
    const val WIRETYPE_FIXED64 = 1
    const val WIRETYPE_LENGTH_DELIMITED = 2
    const val WIRETYPE_START_GROUP = 3
    const val WIRETYPE_END_GROUP = 4
    const val WIRETYPE_FIXED32 = 5

    const val TAG_TYPE_BITS = 3
    const val TAG_TYPE_MASK = (1 shl TAG_TYPE_BITS) - 1

    /** Given a tag value, determines the field number (the upper 29 bits).  */
    fun getTagFieldNumber(tag: Int): Int {
        return tag ushr TAG_TYPE_BITS
    }

    /** Makes a tag value given a field number and wire type.  */
    fun makeTag(fieldNumber: Int, wireType: Int): Int {
        return fieldNumber shl TAG_TYPE_BITS or wireType
    }

    /** Given a tag value, determines the wire type (the lower 3 bits).  */
    fun getTagWireType(tag: Int): Int {
        return tag and WireFormat.TAG_TYPE_MASK
    }
}

/**
 * Decode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
 * into values that can be efficiently encoded with varint.  (Otherwise,
 * negative values must be sign-extended to 64 bits to be varint encoded,
 * thus always taking 10 bytes on the wire.)
 *
 * @param n An unsigned 32-bit integer, stored in a signed int because
 * Java has no explicit unsigned support.
 * @return A signed 32-bit integer.
 */
fun decodeZigZag32(n: Int): Int {
    return n ushr 1 xor -(n and 1)
}

/**
 * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
 * into values that can be efficiently encoded with varint.  (Otherwise,
 * negative values must be sign-extended to 64 bits to be varint encoded,
 * thus always taking 10 bytes on the wire.)
 *
 * @param n An unsigned 64-bit integer, stored in a signed int because
 * Java has no explicit unsigned support.
 * @return A signed 64-bit integer.
 */
fun decodeZigZag64(n: Long): Long {
    return n ushr 1 xor -(n and 1)
}