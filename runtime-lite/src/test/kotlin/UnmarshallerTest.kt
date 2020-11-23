import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jp.co.panpanini.Message
import jp.co.panpanini.Reader
import jp.co.panpanini.Unmarshaller
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt

class UnmarshallerTest {


    private abstract class MockMessage: Message<MockMessage>
    private abstract class MockCompanion: Message.Companion<MockMessage>

    private var reader: Reader = mock { }

    private var discardUnknownFields = false

    private var target = Unmarshaller(reader, discardUnknownFields)

    @Test
    fun `readTag should call stream#readTag`() {
        target.readTag()

        verify(reader).readTag()
    }

    @Test
    fun `readDouble should call stream#readDouble`() {
        target.readDouble()

        verify(reader).readDouble()
    }

    @Test
    fun `readFloat should call stream#readFloat`() {
        target.readFloat()

        verify(reader).readFloat()
    }

    @Test
    fun `readInt32 should call stream#readInt32`() {
        target.readInt32()

        verify(reader).readInt32()
    }

    @Test
    fun `readInt64 should call stream#readInt64`() {
        target.readInt64()

        verify(reader).readInt64()
    }

    @Test
    fun `readUInt32 should call stream#readUInt32`() {
        target.readUInt32()

        verify(reader).readUInt32()
    }

    @Test
    fun `readUInt64 should call stream#readUInt64`() {
        target.readUInt64()

        verify(reader).readUInt64()
    }

    @Test
    fun `readSInt32 should call stream#readSInt32`() {
        target.readSInt32()

        verify(reader).readSInt32()
    }

    @Test
    fun `readSInt64 should call stream#readSInt64`() {
        target.readSInt64()

        verify(reader).readSInt64()
    }

    @Test
    fun `readFixed32 should call stream#readFixed32`() {
        target.readFixed32()

        verify(reader).readFixed32()
    }

    @Test
    fun `readFixed64 should call stream#readFixed64`() {
        target.readFixed64()

        verify(reader).readFixed64()
    }

    @Test
    fun `readSFixed32 should call stream#readSFixed32`() {
        target.readSFixed32()

        verify(reader).readSFixed32()
    }

    @Test
    fun `readSFixed64 should call stream#readSFixed64`() {
        target.readSFixed64()

        verify(reader).readSFixed64()
    }

    @Test
    fun `readBool shuld call stream#readBool`() {
        target.readBool()

        verify(reader).readBool()
    }

    @Test
    fun `readString should call stream#readString`() {
        target.readString()

        verify(reader).readString()
    }

    @Test
    fun `readBytes should call stream#readByteArray`() {
        whenever(reader.readByteArray()).thenReturn(ByteArray(0))

        target.readBytes()

        verify(reader).readByteArray()
    }

    @Test
    fun `readEnum should call stream#readEnum`() {
        whenever(reader.readEnum()).thenReturn(0)

        val companion: Message.Enum.Companion<*> = mock { }

        target.readEnum(companion)

        verify(reader).readEnum()
    }

    @Test
    fun `readEnum should pass the value from stream#readEnum to the companion`() {
        val enumValue = 1
        whenever(reader.readEnum()).thenReturn(enumValue)

        val companion: Message.Enum.Companion<*> = mock { }

        target.readEnum(companion)

        verify(companion).fromValue(enumValue)
    }

    @Test
    fun `readMessage should call stream#pushLimit with the value of stream#readRawVarint32`() {
        val companion: MockCompanion = mock { }
        val previousLimit = 100
        whenever(reader.readRawVarint32()).thenReturn(previousLimit)
        whenever(reader.isAtEnd).thenReturn(true)

        target.readMessage(companion)

        verify(reader).pushLimit(previousLimit)
    }

    @Test
    fun `readMessage should call companion#protoUnmarshal with the current instance`() {
        val companion: MockCompanion = mock { }
        val previousLimit = 100
        whenever(reader.readRawVarint32()).thenReturn(previousLimit)
        whenever(reader.isAtEnd).thenReturn(true)

        target.readMessage(companion)

        verify(companion).protoUnmarshal(target)
    }

    @Test
    fun `readMessage should call stream#popLimit with the previous limit`() {
        val companion: MockCompanion = mock { }
        val previousLimit = 100
        val popLimit = 200
        whenever(reader.readRawVarint32()).thenReturn(previousLimit)
        whenever(reader.pushLimit(anyInt())).thenReturn(popLimit)
        whenever(reader.isAtEnd).thenReturn(true)

        target.readMessage(companion)

        verify(reader).popLimit(popLimit)
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