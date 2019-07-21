import com.google.protobuf.CodedInputStream
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jp.co.panpanini.Message
import jp.co.panpanini.Unmarshaller
import org.junit.Test

class UnmarshallerTest {


    private abstract class MockMessage: Message<MockMessage>
    private abstract class MockCompanion: Message.Companion<MockMessage>

    private var stream: CodedInputStream = mock { }

    private var discardUnknownFields = false

    private var target = Unmarshaller(stream, discardUnknownFields)

    @Test
    fun `readTag should call stream#readTag`() {
        target.readTag()

        verify(stream).readTag()
    }

    @Test
    fun `readDouble should call stream#readDouble`() {
        target.readDouble()

        verify(stream).readDouble()
    }

    @Test
    fun `readFloat should call stream#readFloat`() {
        target.readFloat()

        verify(stream).readFloat()
    }

    @Test
    fun `readInt32 should call stream#readInt32`() {
        target.readInt32()

        verify(stream).readInt32()
    }

    @Test
    fun `readInt64 should call stream#readInt64`() {
        target.readInt64()

        verify(stream).readInt64()
    }

    @Test
    fun `readUInt32 should call stream#readUInt32`() {
        target.readUInt32()

        verify(stream).readUInt32()
    }

    @Test
    fun `readUInt64 should call stream#readUInt64`() {
        target.readUInt64()

        verify(stream).readUInt64()
    }

    @Test
    fun `readSInt32 should call stream#readSInt32`() {
        target.readSInt32()

        verify(stream).readSInt32()
    }

    @Test
    fun `readSInt64 should call stream#readSInt64`() {
        target.readSInt64()

        verify(stream).readSInt64()
    }

    @Test
    fun `readFixed32 should call stream#readFixed32`() {
        target.readFixed32()

        verify(stream).readFixed32()
    }

    @Test
    fun `readFixed64 should call stream#readFixed64`() {
        target.readFixed64()

        verify(stream).readFixed64()
    }

    @Test
    fun `readSFixed32 should call stream#readSFixed32`() {
        target.readSFixed32()

        verify(stream).readSFixed32()
    }

    @Test
    fun `readSFixed64 should call stream#readSFixed64`() {
        target.readSFixed64()

        verify(stream).readSFixed64()
    }

    @Test
    fun `readBool shuld call stream#readBool`() {
        target.readBool()

        verify(stream).readBool()
    }

    @Test
    fun `readString should call stream#readString`() {
        target.readString()

        verify(stream).readString()
    }

    @Test
    fun `readBytes should call stream#readByteArray`() {
        whenever(stream.readByteArray()).thenReturn(ByteArray(0))

        target.readBytes()

        verify(stream).readByteArray()
    }

    @Test
    fun `readEnum should call stream#readEnum`() {
        whenever(stream.readEnum()).thenReturn(0)

        val companion: Message.Enum.Companion<*> = mock { }

        target.readEnum(companion)

        verify(stream).readEnum()
    }

    @Test
    fun `readEnum should pass the value from stream#readEnum to the companion`() {
        val enumValue = 1
        whenever(stream.readEnum()).thenReturn(enumValue)

        val companion: Message.Enum.Companion<*> = mock { }

        target.readEnum(companion)

        verify(companion).fromValue(enumValue)
    }

    @Test
    fun `readMessage should call stream#pushLimit with the value of stream#readRawVarint32`() {
        val companion: MockCompanion = mock { }
        val previousLimit = 100
        whenever(stream.readRawVarint32()).thenReturn(previousLimit)
        whenever(stream.isAtEnd).thenReturn(true)

        target.readMessage(companion)

        verify(stream).pushLimit(previousLimit)
    }

    @Test
    fun readRepeated() {
    }

    @Test
    fun readRepeatedEnum() {
    }

    @Test
    fun readRepeatedMessage() {
    }

    @Test
    fun readMap() {
    }

    @Test
    fun unknownField() {
    }

    @Test
    fun unknownFields() {
    }
}