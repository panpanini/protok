import com.nhaarman.mockitokotlin2.*
import jp.co.panpanini.Message
import jp.co.panpanini.Sizer
import org.assertj.core.api.Assertions.*
import org.junit.Test

import org.mockito.ArgumentMatchers.anyInt

class SizerTest {

    private var target: Sizer = Sizer

    @Test
    fun `tagSize should call uInt32Size with left shifted input`() {
        val input = 100
        target = spy(target)

        target.tagSize(input)

        verify(target).uInt32Size(input shl 3)
    }

    @Test
    fun `int32Size should return 10 if input is less than 0`() {
        val input = -1

        val result = target.int32Size(input)

        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `int32Size should call uInt32Size if input is equal to 0`() {
        val input = 0
        target = spy(target)

        target.int32Size(input)

        verify(target).uInt32Size(input)

    }

    @Test
    fun `int32Size should call uInt32Size if input is greater than 0`() {
        val input = 1
        target = spy(target)

        target.int32Size(input)

        verify(target).uInt32Size(input)
    }

    @Test
    fun `uInt32Size should return 1 when input is less than 7 bits long`() {
        val input = 127
        val expected = 1

        val result = target.uInt32Size(input)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `uInt32Size should return 2 when input is more than 7 bits and less than 14 bits long`() {
        val input = 16383
        val expected = 2

        val result = target.uInt32Size(input)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `uInt32Size should return 3 when input is more than 14 bits and less than 21 bits long`() {
        val input = 2097151
        val expected = 3

        val result = target.uInt32Size(input)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `uInt32Size should return 4 when input is more than 21 bits and less than 28 bits long`() {
        val input = 268435455
        val expected = 4

        val result = target.uInt32Size(input)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `uInt32Size should return 5 when input is more than 28 bits long`() {
        val input = 268435456
        val expected = 5

        val result = target.uInt32Size(input)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enumSize should call int32Size`() {
        val input: Enum<*> = mock {
            whenever(mock.ordinal).thenReturn(1)
        }
        target = spy(target)

        target.enumSize(input)

        verify(target).int32Size(input.ordinal)
    }

    @Test
    fun `messageSize should call uInt32Size with the message protoSize`() {
        val protoSize = 10
        val input: Message<*> = mock {
            whenever(mock.protoSize).thenReturn(protoSize)
        }
        target = spy(target)

        target.messageSize(input)

        verify(target).uInt32Size(protoSize)
    }

    @Test
    fun `messageSize should return the protosize + uInt32Size of the protoSize`() {
        val protoSize = 10
        val sizeOfProtoSize = 1 // less than 128 == 1 byte
        val input: Message<*> = mock {
            whenever(mock.protoSize).thenReturn(protoSize)
        }

        val result = target.messageSize(input)

        assertThat(result).isEqualTo(protoSize + sizeOfProtoSize)
    }

    @Test
    fun `packedRepeatedSize should return 1 for an empty list`() {
        val input: List<Int> = listOf()
        val sizingFunction: (Int) -> Int = {
            target.int32Size(it)
        }

        val result = target.packedRepeatedSize(input, sizingFunction)

        assertThat(result).isEqualTo(1)
    }

    @Test
    fun `packedRepeatedSize should call sizingFunction to find size of items`() {
        val input: List<Int> = listOf(1)
        val sizingFunction: (Int) -> Int = mock {
            whenever(mock.invoke(anyInt())).thenReturn(1)
        }

        target.packedRepeatedSize(input, sizingFunction)

        verify(sizingFunction).invoke(1)
    }

    @Test
    fun `packedRepeatedSize should return the value of sizingFunction for each item, and add the size of the protoSize`() {
        val input: List<Int> = listOf(127, 16383, 2097151) // int32Size = 1, 2, 3
        val protoSize = 1
        val sizingFunction: (Int) -> Int = {
            target.int32Size(it)
        }

        val result = target.packedRepeatedSize(input, sizingFunction)

        assertThat(result).isEqualTo(1 + 2 + 3 + protoSize)
    }

    @Test
    fun `mapSize should call tagSize with the fieldNumber`() {
        val fieldNumber = 1
        val map: Map<String, String> = mock { }
        val createEntry: (String, String) -> Nothing = mock { }
        target = spy(target)

        target.mapSize(fieldNumber, map, createEntry)

        verify(target).tagSize(fieldNumber)
    }

    @Test
    fun `mapSize should sum all entries when map contains messages`() {
        abstract class MapEntry<T: Message<T>>: Message<T>, Map.Entry<Int, T>
        val fieldNumber = 1

        val protoSize = 10
        val sizeOfProtoSize = 1 // less than 128 == 1 byte
        val input: MapEntry<*> = mock {
            whenever(mock.protoSize).thenReturn(protoSize)
        }

        val map: Map<Int, Message<*>> = mock {
            whenever(mock.entries).thenReturn(setOf(input))
        }
        val createEntry: (Int, Message<*>) -> Nothing = mock { }

        val result = target.mapSize(fieldNumber, map, createEntry)

        assertThat(result).isEqualTo(protoSize + sizeOfProtoSize + 1) // sizeof fieldNumber
    }

    @Test
    fun `int64Size should call uInt64Size`() {
        val input = 1L
        target = spy(target)

        target.int64Size(input)

        verify(target).uInt64Size(input)
    }


    @Test
    fun `bytesSize should call uInt32Size with the array size`() {
        val size = 10
        val input = ByteArray(size)
        target = spy(target)

        target.bytesSize(input)

        verify(target).uInt32Size(size)
    }

    @Test
    fun `bytesSize should return uInt32Size of the array size + array size`() {
        val size = 10
        val tagSize = 1 // size is less than 127
        val input = ByteArray(size)

        val result = target.bytesSize(input)

        assertThat(result).isEqualTo(size + tagSize)
    }

    @Test
    fun `sInt32Size should call uInt32Size with zigzag encoded input`() {
        val input = 10
        val zigZagEncoded = (input shl 1) xor (input shr 31)
        target = spy(target)

        target.sInt32Size(input)

        verify(target).uInt32Size(zigZagEncoded)
    }

    @Test
    fun `sInt64Size should call uInt64Size with zigzag encoded input`() {
        val input = 10L
        val zigZagEncoded = (input shl 1) xor (input shr 63)
        target = spy(target)

        target.sInt64Size(input)

        verify(target).uInt64Size(zigZagEncoded)
    }

    @Test
    fun `doubleSize should equal 8`() {
        assertThat(target.doubleSize()).isEqualTo(8)
    }

    @Test
    fun `floatSize should equal 4`() {
        assertThat(target.floatSize()).isEqualTo(4)
    }

    @Test
    fun `fixed32Size should equal 4`() {
        assertThat(target.fixed32Size()).isEqualTo(4)
    }

    @Test
    fun `fixed64Size should equal 8`() {
        assertThat(target.fixed64Size()).isEqualTo(8)
    }

    @Test
    fun `sFixed32Size should equal 4`() {
        assertThat(target.sFixed32Size()).isEqualTo(4)
    }

    @Test
    fun `sFixed64Size should equal 8`() {
        assertThat(target.sFixed64Size()).isEqualTo(8)
    }

    @Test
    fun `boolSize should equal 1`() {
        assertThat(target.boolSize()).isEqualTo(1)
    }

    @Test
    fun stringSize() {
    }
}