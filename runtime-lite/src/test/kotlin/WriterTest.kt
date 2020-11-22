package jp.co.panpanini

import Utf8
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

class WriterTest {

    private val utf8: Utf8 = mock { }

    private lateinit var byteArray: ByteArray

    private lateinit var target: Writer


    @Before
    fun setup() {
        byteArray = ByteArray(100)
        target = spy(Writer(byteArray, utf8))
    }

    fun setup(size: Int) {
        byteArray = ByteArray(size)
        target = spy(Writer(byteArray))
    }

    @Test
    fun `writeTag should write the UInt32 representation to byteArray`() {
        val input = 150

        target.writeTag(input)

        verify(target).writeUInt32(input)
    }

    @Test
    fun `writeInt32 should writeUInt32 value when value is greater than 0`() {
        val input = 150

        target.writeInt32(input)

        verify(target).writeUInt32(input)
    }

    @Test
    fun `writeInt32 should writeUInt64 value when value is less than 0`() {
        val input = -150
        target.writeInt32(input)

        verify(target).writeUInt64(input.toLong())
    }

    @Test
    fun `writeInt64 should call writeUInt64`() {
        val input = 1L

        target.writeInt64(input)

        verify(target).writeUInt64(input)
    }

    @Test
    fun `writeSInt32 should call writeUInt32 with zigzag encoded value`() {
        val input = 1

        target.writeSInt32(input)

        verify(target).writeUInt32(input.encodeZigZag32())
    }

    @Test
    fun `writeSInt64 should call writeUInt64 with zigzag encoded value`() {
        val input = 1L
        target.writeSInt64(input)

        verify(target).writeUInt64(input.encodeZigZag64())
    }

    @Test
    fun `writeSFixed32 should call writeFixed32`() {
        val input = 1

        target.writeSFixed32(input)

        verify(target).writeFixed32(input)
    }

    @Test
    fun `writeSFixed64 should call writeFixed64`() {
        val input = 1L

        target.writeSFixed64(input)

        verify(target).writeFixed64(input)
    }

    @Test
    fun `writeBool should call writeByte`() {
        var input = true

        target.writeBool(input)

        verify(target).writeByte(1.toByte())

        input = false

        target.writeBool(input)

        verify(target).writeByte(0.toByte())
    }

    @Test
    fun `writeDouble should call writeFixed64 with raw bits`() {
        val input = 1.0

        target.writeDouble(input)

        verify(target).writeFixed64(input.toRawBits())
    }

    @Test
    fun `writeFloat should call writeFixed32 with raw bits`() {
        val input = 1f

        target.writeFloat(input)

        verify(target).writeFixed32(input.toRawBits())
    }

    @Test
    fun `writeByte should write the byte to byteArray`() {
        setup(1)
        val input = 2.toByte()

        target.writeByte(input)

        assertThat(byteArray[0]).isEqualTo(input)
    }

    @Test
    fun `writeBytes should call writeByte for each value in the byte array`() {
        val byte1 = 1.toByte()
        val byte2 = 2.toByte()
        val input = byteArrayOf(byte1, byte2)

        target.writeBytes(input)

        verify(target).writeByte(byte1)
        verify(target).writeByte(byte2)
    }

    @Test
    fun `complete should return the byteArray`() {
        assertThat(target.complete()).isSameAs(byteArray)
    }

    @Test
    fun `writeUInt32 should write 7bit integer in 1 byte`() {
        setup(1)
        val input = 127 // largest 7-bit integer
        val expected = byteArrayOf(0b01111111.toByte())

        target.writeUInt32(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeUInt32 should write 32-bit integer in 5 bytes`() {
        setup(5)
        val input = Integer.MAX_VALUE // largest 32-bit integer
        val expected = byteArrayOf(
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b00000111.toByte()
        )

        target.writeUInt32(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeUInt64 should write 7bit long in 1 byte`() {
        setup(1)
        val input = 127L // largest 7-bit integer
        val expected = byteArrayOf(0b01111111.toByte())

        target.writeUInt64(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeUInt64 should write 32-bit integer in 5 bytes`() {
        setup(5)
        val input = Integer.MAX_VALUE.toLong() // largest 32-bit integer
        val expected = byteArrayOf(
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b00000111.toByte()
        )

        target.writeUInt64(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeUInt64 should write 64-bit integer in 9 bytes`() {
        setup(9)
        val input = Long.MAX_VALUE // largest 64-bit integer
        val expected = byteArrayOf(
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b11111111.toByte(),
                0b01111111.toByte()
        )

        target.writeUInt64(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeFixed32 should write 4 bytes exactly`() {
        setup(4)
        val input = 2140483647
        val expected = byteArrayOf(
                0b00111111.toByte(),
                0b00110000.toByte(),
                0b10010101.toByte(),
                0b01111111.toByte()
        )

        target.writeFixed32(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeFixed64 should write 8 bytes exactly`() {
        setup(8)
        val input = 9223302036854775807L
        val expected = byteArrayOf(
                0b11111111.toByte(),
                0b10011111.toByte(),
                0b11011101.toByte(),
                0b11011010.toByte(),
                0b01010101.toByte(),
                0b11000000.toByte(),
                0b11111111.toByte(),
                0b01111111.toByte()
        )

        target.writeFixed64(input)

        assertThat(byteArray).isEqualTo(expected)
    }

    @Test
    fun `writeString should call utf8#encode`() {
        val input = "Hello, World!"
        val computedLength = 13

        whenever(utf8.encode(anyString(), any(), anyInt(), anyInt())).thenReturn(computedLength)

        target.writeString(input)

        verify(target).writeUInt32(computedLength - 1) // string length int is 1 byte
        verify(utf8).encode(input, byteArray, 1, 99)
    }

    @Test
    fun `writeString should call utf8#encodedLength for a long string`() {
        val input = (0..1000).joinToString { "test$it " }

        target.writeString(input)

        verify(utf8).encodedLength(input) // ask utf8 for the right length
        verify(utf8).encode(input, byteArray, 1, 99)
    }

}