package jp.co.panpanini

import com.google.protobuf.CodedInputStream
import java.io.InputStream
import java.io.Serializable

actual interface Message<T : Message<T>> : Serializable {
    actual fun protoUnmarshal(u: Unmarshaller): T
    fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    fun protoUnmarshal(inputStream: InputStream) = protoUnmarshal(Unmarshaller(CodedInputStream.newInstance(inputStream)))

    actual operator fun plus(other: T?): T
    actual val protoSize: Int
    actual fun protoMarshal(m: Marshaller)
    fun protoMarshal(): ByteArray = Marshaller.allocate(protoSize).also(::protoMarshal).complete()!!

    actual interface Companion<T : Message<T>> {
        actual fun protoUnmarshal(u: Unmarshaller): T
        fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    }

    actual interface Enum : Serializable {
        actual val value: Int

        actual interface Companion<T : Enum> {
            actual fun fromValue(value: Int): T
        }
    }
}

