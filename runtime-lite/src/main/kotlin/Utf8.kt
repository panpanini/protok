internal object Utf8 {
    const val MAX_BYTES_PER_CHAR = 3

    internal class UnpairedSurrogateException(index: Int, length: Int) :
            IllegalArgumentException("Unpaired surrogate at index $index of $length")

    fun encode(input: CharSequence, out: ByteArray, offset: Int, length: Int): Int {
        val utf16Length = input.length
        var j = offset
        var i = 0
        val limit = offset + length
        // Designed to take advantage of
        // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
        run {
            var c: Char? = null
            while (i < utf16Length && i + j < limit && input[i].also { c = it }.toInt() < 0x80) {
                out[j + i] = c!!.toByte() //TODO: can we do this without !!
                i++
            }
        }
        if (i == utf16Length) {
            return j + utf16Length
        }
        j += i
        var c: Char
        while (i < utf16Length) {
            c = input[i]
            if (c.toInt() < 0x80 && j < limit) {
                out[j++] = c.toByte()
            } else if (c.toInt() < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                out[j++] = (0xF shl 6 or (c.toInt() ushr 6)).toByte()
                out[j++] = (0x80 or (0x3F and c.toInt())).toByte()
            } else if ((c < Char.MIN_SURROGATE || Char.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                out[j++] = (0xF shl 5 or (c.toInt() ushr 12)).toByte()
                out[j++] = (0x80 or (0x3F and (c.toInt() ushr 6))).toByte()
                out[j++] = (0x80 or (0x3F and c.toInt())).toByte()
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits,
                // four UTF-8 bytes
                var low: Char? = null
                if (i + 1 == input.length
                        || !Character.isSurrogatePair(c, input[++i].also { low = it })) {
                    throw UnpairedSurrogateException(i - 1, utf16Length)
                }
                val codePoint: Int = Character.toCodePoint(c, low!!) //TODO: can I do this without !!
                out[j++] = (0xF shl 4 or (codePoint ushr 18)).toByte()
                out[j++] = (0x80 or (0x3F and (codePoint ushr 12))).toByte()
                out[j++] = (0x80 or (0x3F and (codePoint ushr 6))).toByte()
                out[j++] = (0x80 or (0x3F and codePoint)).toByte()
            } else {
                // If we are surrogates and we're not a surrogate pair, always throw an
                // UnpairedSurrogateException instead of an ArrayOutOfBoundsException.
                if (Char.MIN_SURROGATE <= c && c <= Char.MAX_SURROGATE
                        && (i + 1 == input.length
                                || !Character.isSurrogatePair(c, input[i + 1]))) {
                    throw UnpairedSurrogateException(i, utf16Length)
                }
                throw IndexOutOfBoundsException("Failed writing $c at index $j")
            }
            i++
        }
        return j
    }

    /**
     * Returns the number of bytes in the UTF-8-encoded form of `sequence`. For a string,
     * this method is equivalent to `string.getBytes(UTF_8).length`, but is more efficient in
     * both time and space.
     *
     * @throws IllegalArgumentException if `sequence` contains ill-formed UTF-16 (unpaired
     * surrogates)
     */
    fun encodedLength(sequence: CharSequence): Int {
        // Warning to maintainers: this implementation is highly optimized.
        val utf16Length = sequence.length
        var utf8Length = utf16Length
        var i = 0

        // This loop optimizes for pure ASCII.
        while (i < utf16Length && sequence[i].toInt() < 0x80) {
            i++
        }

        // This loop optimizes for chars less than 0x800.
        while (i < utf16Length) {
            val c = sequence[i]
            if (c.toInt() < 0x800) {
                utf8Length += 0x7f - c.toInt() ushr 31 // branch free!
            } else {
                utf8Length += encodedLengthGeneral(sequence, i)
                break
            }
            i++
        }
        if (utf8Length < utf16Length) {
            // Necessary and sufficient condition for overflow because of maximum 3x expansion
            throw IllegalArgumentException("UTF-8 length does not fit in int: "
                    + (utf8Length + (1L shl 32)))
        }
        return utf8Length
    }

    private fun encodedLengthGeneral(sequence: CharSequence, start: Int): Int {
        val utf16Length = sequence.length
        var utf8Length = 0
        var i = start
        while (i < utf16Length) {
            val c = sequence[i]
            if (c.toInt() < 0x800) {
                utf8Length += 0x7f - c.toInt() ushr 31 // branch free!
            } else {
                utf8Length += 2
                // jdk7+: if (Character.isSurrogate(c)) {
                if (Char.MIN_SURROGATE <= c && c <= Char.MAX_SURROGATE) {
                    // Check that we have a well-formed surrogate pair.
                    val cp: Int = Character.codePointAt(sequence, i)
                    if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        throw UnpairedSurrogateException(i, utf16Length)
                    }
                    i++
                }
            }
            i++
        }
        return utf8Length
    }
}

object Character {

    const val MIN_SUPPLEMENTARY_CODE_POINT = 0x010000

    fun isSurrogatePair(high: Char, low: Char): Boolean {
        return isHighSurrogate(high) && isLowSurrogate(low)
    }

    fun isHighSurrogate(ch: Char): Boolean {
        return ch >= Char.MIN_HIGH_SURROGATE && ch.toInt() < Char.MAX_HIGH_SURROGATE.toInt() + 1
    }

    fun isLowSurrogate(ch: Char): Boolean {
        return ch >= Char.MIN_LOW_SURROGATE && ch.toInt() < Char.MAX_LOW_SURROGATE.toInt() + 1
    }

    fun toCodePoint(high: Char, low: Char): Int {
        // Optimized form of:
        // return ((high - MIN_HIGH_SURROGATE) << 10)
        //         + (low - MIN_LOW_SURROGATE)
        //         + MIN_SUPPLEMENTARY_CODE_POINT;
        return (high.toInt() shl 10) + low.toInt() + (MIN_SUPPLEMENTARY_CODE_POINT
                - (Char.MIN_HIGH_SURROGATE.toInt() shl 10)
                - Char.MIN_LOW_SURROGATE.toInt())
    }

    fun codePointAt(seq: CharSequence, index: Int): Int {
        var index = index
        val c1 = seq[index]
        if (isHighSurrogate(c1) && ++index < seq.length) {
            val c2 = seq[index]
            if (isLowSurrogate(c2)) {
                return toCodePoint(c1, c2)
            }
        }
        return c1.toInt()
    }
}