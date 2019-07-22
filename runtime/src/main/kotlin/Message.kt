package jp.co.panpanini

import com.google.protobuf.CodedInputStream
import java.io.InputStream

interface Message<T : Message<T>> {
    fun protoUnmarshal(u: Unmarshaller): T
    fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    fun protoUnmarshal(inputStream: InputStream) = protoUnmarshal(Unmarshaller(CodedInputStream.newInstance(inputStream)))

    operator fun plus(other: T?): T
    val protoSize: Int
    fun protoMarshal(m: Marshaller)
    fun protoMarshal(): ByteArray = Marshaller.allocate(protoSize).also(::protoMarshal).complete()!!

    interface Companion<T : Message<T>> {
        fun protoUnmarshal(u: Unmarshaller): T
        fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    }

    interface Enum {
        val value: Int

        interface Companion<T : Enum> {
            fun fromValue(value: Int): T
        }
    }
}

