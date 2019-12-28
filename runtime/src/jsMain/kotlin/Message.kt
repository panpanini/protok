package jp.co.panpanini

actual interface Message<T: Message<T>> {
    actual fun protoUnmarshal(u: Unmarshaller): T
    actual fun protoMarshal(m: Marshaller)
    actual operator fun plus(other: T?): T
    actual val protoSize: Int

    actual interface Enum {
        actual val value: Int

        actual interface Companion<T : Enum> {
            actual fun fromValue(value: Int): T
        }

    }

    actual interface Companion<T : Message<T>> {
        actual fun protoUnmarshal(u: Unmarshaller): T
    }
}