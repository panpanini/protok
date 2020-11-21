import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jp.co.panpanini.ByteArr
import jp.co.panpanini.Marshaller
import jp.co.panpanini.Message
import jp.co.panpanini.Writer
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt

class MarshallerTest {

    private var stream: Writer = mock { }

    private var target: Marshaller = Marshaller(stream)

    @Test
    fun `writeTag should call stream#writeTag`() {
        val input = 100

        target.writeTag(input)

        verify(stream).writeTag(input)
    }

    @Test
    fun `writeTag should shl the fieldNum by three and || with the wireType`() {
        val fieldNum = 1
        val wireType = 2
        val fieldNumSHL3 = 8

        target.writeTag(fieldNum, wireType)


        verify(stream).writeTag(fieldNumSHL3 + wireType)
    }

    @Test
    fun `writeDouble should call stream#writeDouble`() {
        val input = 1.0

        target.writeDouble(input)

        verify(stream).writeDouble(input)
    }

    @Test
    fun `writeFloat should call stream#writeFloat`() {
        val input = 1f

        target.writeFloat(input)

        verify(stream).writeFloat(input)
    }

    @Test
    fun `writeInt32 should call stream#writeInt32`() {
        val input = 1

        target.writeInt32(input)

        verify(stream).writeInt32(input)
    }

    @Test
    fun `writeInt64 should call stream#writeInt64`() {
        val input = 1L

        target.writeInt64(input)

        verify(stream).writeInt64(input)
    }

    @Test
    fun `writeUInt32 should call stream#writeUInt32`() {
        val input = 1

        target.writeUInt32(input)

        verify(stream).writeUInt32(input)
    }

    @Test
    fun `writeUInt64 should call stream#writeUInt64`() {
        val input = 1L

        target.writeUInt64(input)

        verify(stream).writeUInt64(input)
    }

    @Test
    fun `writeSInt32 should call stream#writeSInt32`() {
        val input = 1

        target.writeSInt32(input)

        verify(stream).writeSInt32(input)
    }

    @Test
    fun `writeSInt64 should call stream#writeSInt64`() {
        val input = 1L

        target.writeSInt64(input)

        verify(stream).writeSInt64(input)
    }

    @Test
    fun `writeFixed32 should call stream#writeFixed32`() {
        val input = 1

        target.writeFixed32(input)

        verify(stream).writeFixed32(input)
    }

    @Test
    fun `writeFixed64 should call stream#writeFixed64`() {
        val input = 1L

        target.writeFixed64(input)

        verify(stream).writeFixed64(input)
    }

    @Test
    fun `writeSFixed32 should call stream#writeSFixed32`() {
        val input = 1

        target.writeSFixed32(input)

        verify(stream).writeSFixed32(input)
    }

    @Test
    fun `writeSFixed64 should call stream#writeSFixed64`() {
        val input = 1L

        target.writeSFixed64(input)

        verify(stream).writeSFixed64(input)
    }

    @Test
    fun `writeBool should call stream#writeBool`() {
        val input = true

        target.writeBool(input)

        verify(stream).writeBool(input)
    }

    @Test
    fun `writeString should call stream#writeString`() {
        val input = ""

        target.writeString("")

        verify(stream).writeString(input)
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
    fun `writeBytes should call stream#writeByteArray`() {
        val input = ByteArray(0)

        target.writeBytes(input)

        verify(stream).writeBytes(input)
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
    fun `complete should call writer#complete`() {
        target.complete()

        verify(stream).complete()
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