// Code generated by protok protocol buffer plugin, do not edit.
// Source file: oneoftest.proto
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

data class Item(@JvmField val id: String = "", val unknownFields: Map<Int, UnknownField> =
        emptyMap()) : Message<Item>, Serializable {
    override val protoSize: Int = protoSizeImpl()


    constructor(id: String) : this(id, emptyMap())

    fun Item.protoSizeImpl(): Int {
        var protoSize = 0
        if (id != DEFAULT_ID) {
            protoSize += jp.co.panpanini.Sizer.tagSize(1) + jp.co.panpanini.Sizer.stringSize(id)
        }
        protoSize += unknownFields.entries.sumBy { it.value.size() }
        return protoSize
    }

    fun Item.protoMarshalImpl(protoMarshal: Marshaller) {
        if (id != DEFAULT_ID) {
            protoMarshal.writeTag(10).writeString(id)

        }
        if (unknownFields.isNotEmpty()) {
            protoMarshal.writeUnknownFields(unknownFields)
        }
    }

    fun Item.protoMergeImpl(other: Item?): Item = other?.copy(
        unknownFields = unknownFields + other.unknownFields
    ) ?: this

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: Item?): Item = protoMergeImpl(other)

    fun encode(): ByteArray = protoMarshal()

    override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Item =
            Companion.protoUnmarshal(protoUnmarshal)

    fun newBuilder(): Builder = Builder()
        .id(id)
        .unknownFields(unknownFields)

    companion object : Message.Companion<Item> {
        @JvmField
        val DEFAULT_ID: String = ""

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Item {
            var id = ""
            while (true) {
                when (protoUnmarshal.readTag()) {
                    0 -> return Item(id, protoUnmarshal.unknownFields())
                    10 -> id = protoUnmarshal.readString()
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): Item = protoUnmarshal(arr)
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

        fun build(): Item = Item(id, unknownFields)
    }
}
