package api

import java.io.Serializable
import jp.co.panpanini.Message
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import pbandk.Marshaller
import pbandk.UnknownField
import pbandk.Unmarshaller

data class Thing(@JvmField val id: String = "", val unknownFields: Map<Int, UnknownField> =
        emptyMap()) : Message<Thing>, Serializable {
    override val protoSize: Int = protoSizeImpl()


    constructor(id: String) : this(id, emptyMap())

    fun Thing.protoSizeImpl(): Int {
        var protoSize = 0
        if (id != DEFAULT_ID) {
            protoSize += pbandk.Sizer.tagSize(1) + pbandk.Sizer.stringSize(id)
        }
        protoSize += unknownFields.entries.sumBy {
                 it.value.size()
                 }
        return protoSize
    }

    fun Thing.protoMarshalImpl(protoMarshal: Marshaller) {
        if (id != DEFAULT_ID) {
            protoMarshal.writeTag(10).writeString(id)

        }
        if (unknownFields.isNotEmpty()) {
            protoMarshal.writeUnknownFields(unknownFields)
        }
    }

    fun Thing.protoMergeImpl(other: Thing?): Thing {
        val obj = other?.copy(
        unknownFields = unknownFields + other.unknownFields
        ) ?: this
        return obj
    }

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: Thing?): Thing = protoMergeImpl(other)

    fun encode(): ByteArray = protoMarshal()

    override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Thing =
            Companion.protoUnmarshal(protoUnmarshal)
    fun newBuilder(): Builder {
        val builder =  Builder()
            .id(id)
            .unknownFields(unknownFields)
        return builder
    }

    companion object : Message.Companion<Thing> {
        @JvmField
        val DEFAULT_ID: String = ""

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Thing {
            var id = ""
            while (true) {
                when (protoUnmarshal.readTag()) {
                    0 -> return Thing(
                            id!!, protoUnmarshal.unknownFields()
                            )
                    10 -> id = protoUnmarshal.readString()
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): Thing = protoUnmarshal(arr)
    }

    class Builder {
        var id: String = DEFAULT_ID

        var unknownFields: Map<Int, UnknownField> = emptyMap()

        fun id(id: String?): Builder {
            this.id = id ?: DEFAULT_ID
            return this
        }

        fun unknownFields(unknownFields: Map<Int, UnknownField>): Builder {
            this.unknownFields = unknownFields
            return this
        }

        fun build(): Thing {
            val obj = Thing(id, unknownFields)
            return obj
        }
    }
}
