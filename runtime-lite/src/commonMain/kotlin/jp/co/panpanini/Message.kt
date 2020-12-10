package jp.co.panpanini

interface Message<T : Message<T>> : Serializable {
    fun protoUnmarshal(u: Unmarshaller): T
    fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))

    operator fun plus(other: T?): T
    val protoSize: Int
    fun protoMarshal(m: Marshaller)
    fun protoMarshal(): ByteArray = Marshaller.allocate(protoSize).also(::protoMarshal).complete()

    interface Companion<T : Message<T>> {
        fun protoUnmarshal(u: Unmarshaller): T
        fun protoUnmarshal(arr: ByteArray) = protoUnmarshal(Unmarshaller.fromByteArray(arr))
    }

    interface Enum : Serializable {
        val value: Int

        interface Companion<T : Enum> {
            fun fromValue(value: Int): T
        }
    }
}