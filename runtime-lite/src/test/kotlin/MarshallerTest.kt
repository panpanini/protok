import com.google.protobuf.CodedOutputStream
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jp.co.panpanini.ByteArr
import jp.co.panpanini.Marshaller
import jp.co.panpanini.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt

class MarshallerTest {


    private var stream: CodedOutputStream = mock { }

    private var bytes: ByteArray? = ByteArray(0)

    private var target: Marshaller = Marshaller(stream, bytes)

    @Test
    fun `writeTag should call stream#writeInt32NoTag`() {
        val input = 100

        target.writeTag(input)

        verify(stream).writeInt32NoTag(input)
    }

    @Test
    fun `writeTag should shl the fieldNum by three and || with the wireType`() {
        val fieldNum = 1
        val wireType = 2
        val fieldNumSHL3 = 8

        target.writeTag(fieldNum, wireType)


        verify(stream).writeInt32NoTag(fieldNumSHL3 + wireType)
    }

    @Test
    fun `writeDouble should call stream#writeDoubleNoTag`() {
        val input = 1.0

        target.writeDouble(input)

        verify(stream).writeDoubleNoTag(input)
    }

    @Test
    fun `writeFloat should call stream#writeFloatNoTag`() {
        val input = 1f

        target.writeFloat(input)

        verify(stream).writeFloatNoTag(input)
    }

    @Test
    fun `writeInt32 should call stream#writeInt32NoTag`() {
        val input = 1

        target.writeInt32(input)

        verify(stream).writeInt32NoTag(input)
    }

    @Test
    fun `writeInt64 should call stream#writeInt64NoTag`() {
        val input = 1L

        target.writeInt64(input)

        verify(stream).writeInt64NoTag(input)
    }

    @Test
    fun `writeUInt32 should call stream#writeUInt32NoTag`() {
        val input = 1

        target.writeUInt32(input)

        verify(stream).writeUInt32NoTag(input)
    }

    @Test
    fun `writeUInt64 should call stream#writeUInt64NoTag`() {
        val input = 1L

        target.writeUInt64(input)

        verify(stream).writeUInt64NoTag(input)
    }

    @Test
    fun `writeSInt32 should call stream#writeSInt32NoTag`() {
        val input = 1

        target.writeSInt32(input)

        verify(stream).writeSInt32NoTag(input)
    }

    @Test
    fun `writeSInt64 should call stream#writeSInt64NoTag`() {
        val input = 1L

        target.writeSInt64(input)

        verify(stream).writeSInt64NoTag(input)
    }

    @Test
    fun `writeFixed32 should call stream#writeFixed32NoTag`() {
        val input = 1

        target.writeFixed32(input)

        verify(stream).writeFixed32NoTag(input)
    }

    @Test
    fun `writeFixed64 should call stream#writeFixed64NoTag`() {
        val input = 1L

        target.writeFixed64(input)

        verify(stream).writeFixed64NoTag(input)
    }

    @Test
    fun `writeSFixed32 should call stream#writeSFixed32NoTag`() {
        val input = 1

        target.writeSFixed32(input)

        verify(stream).writeSFixed32NoTag(input)
    }

    @Test
    fun `writeSFixed64 should call stream#writeSFixed64NoTag`() {
        val input = 1L

        target.writeSFixed64(input)

        verify(stream).writeSFixed64NoTag(input)
    }

    @Test
    fun `writeBool should call stream#writeBoolNoTag`() {
        val input = true

        target.writeBool(input)

        verify(stream).writeBoolNoTag(input)
    }

    @Test
    fun `writeString should call stream#writeStringNoTag`() {
        val input = ""

        target.writeString("")

        verify(stream).writeStringNoTag(input)
    }

    @Test
    fun `writeBytes should call writeBytes with byte array`() {
        val input = ByteArray(0)
        val byteArr: ByteArr = mock {
            whenever(mock.array).thenReturn(input)
        }
        target = spy(target)

        target.writeBytes(byteArr)

        verify(target).writeBytes(input)
    }

    @Test
    fun `writeBytes should call stream#writeByteArrayNoTag`() {
        val input = ByteArray(0)

        target.writeBytes(input)

        verify(stream).writeByteArrayNoTag(input)
    }

    @Test
    fun `writeMessage should call writeUInt32 with the message proto size`() {
        val protoSize = 10
        val input: Message<*> = mock {
            whenever(mock.protoSize).thenReturn(protoSize)
        }
        target = spy(target)

        target.writeMessage(input)

        verify(target).writeUInt32(protoSize)
    }

    @Test
    fun `writeMessage should call message#protoMarshal with the current instance`() {
        val protoSize = 10
        val input: Message<*> = mock {
            whenever(mock.protoSize).thenReturn(protoSize)
        }

        target.writeMessage(input)

        verify(input).protoMarshal(target)
    }

    @Test
    fun `complete should return the byte array`() {

        val result = target.complete()

        assertThat(result).isEqualTo(bytes)
    }

    @Test
    fun `writeMap should call writeTag for each entry`() {
        abstract class MapEntry<T: Message<T>>: Message<T>, Map.Entry<Int, T>
        val entry: MapEntry<*> = mock { }
        val tag = 1
        val map: Map<Int, Message<*>> = mock {
            whenever(mock.entries).thenReturn(setOf(entry))
        }
        val createEntry: (Int, Message<*>) -> Nothing = mock { }
        target = spy(target)

        target.writeMap(tag, map, createEntry)

        verify(target).writeTag(tag)
    }

    @Test
    fun `writeMap should call writeMessage for each entry`() {
        abstract class MapEntry<T: Message<T>>: Message<T>, Map.Entry<Int, T>
        val entry: MapEntry<*> = mock { }
        val tag = 1
        val map: Map<Int, Message<*>> = mock {
            whenever(mock.entries).thenReturn(setOf(entry))
        }
        val createEntry: (Int, Message<*>) -> Nothing = mock { }
        target = spy(target)

        target.writeMap(tag, map, createEntry)

        verify(target).writeMessage(entry)
    }

    @Test
    fun `writePackedRepeated should call writeUInt32 with the sum of the sizes`() {
        val input = listOf(1)
        val sizeFunction: (Int) -> Int = { it }
        val writeFunction: (Int) -> Unit = { }
        target = spy(target)

        target.writePackedRepeated(input, sizeFunction, writeFunction)

        verify(target).writeUInt32(1)
    }

    @Test
    fun `writePackedRepeated should call the size function for each item`() {
        val input = listOf(1, 2, 3)
        val sizeFunction: (Int) -> Int = mock {
            whenever(mock.invoke(anyInt())).thenReturn(1)
        }
        val writeFunction: (Int) -> Unit = { }

        target.writePackedRepeated(input, sizeFunction, writeFunction)

        verify(sizeFunction).invoke(1)
        verify(sizeFunction).invoke(2)
        verify(sizeFunction).invoke(3)
    }

    @Test
    fun `writePackedRepeated should call the write function for each item`() {
        val input = listOf(1, 2, 3)
        val sizeFunction: (Int) -> Int = { it }

        val writeFunction: (Int) -> Unit = mock { }

        target.writePackedRepeated(input, sizeFunction, writeFunction)

        verify(writeFunction).invoke(1)
        verify(writeFunction).invoke(2)
        verify(writeFunction).invoke(3)
    }

    @Test
    fun `writeEnum should call writeInt32`() {
        val value = 1
        val enum: Message.Enum = mock {
            whenever(mock.value).thenReturn(value)
        }
        target = spy(target)

        target.writeEnum(enum)

        verify(target).writeInt32(value)
    }
}