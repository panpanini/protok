package jp.co.panpanini

import com.google.protobuf.CodedInputStream
import pbandk.Message
import pbandk.Unmarshaller
import java.io.InputStream

interface Message<T : jp.co.panpanini.Message<T>> : Message<T> {
    fun protoUnmarshal(u: Unmarshaller): T
    fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    fun protoUnmarshal(inputStream: InputStream) = protoUnmarshal(Unmarshaller(CodedInputStream.newInstance(inputStream)))

    interface Companion<T : jp.co.panpanini.Message<T>> : Message.Companion<T>
}

