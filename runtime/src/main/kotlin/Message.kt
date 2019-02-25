package jp.co.panpanini

import pbandk.Message
import pbandk.Unmarshaller

interface Message<T : jp.co.panpanini.Message<T>> : Message<T> {
    fun protoUnmarshal(u: Unmarshaller): T
    fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))

    interface Companion<T : jp.co.panpanini.Message<T>> : Message.Companion<T>
}

