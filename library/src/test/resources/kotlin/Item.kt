// Code generated by protok protocol buffer plugin, do not edit.
// Source file: oneoftest.proto
package api

import java.io.Serializable
import jp.co.panpanini.Marshaller
import jp.co.panpanini.Message
import jp.co.panpanini.UnknownField
import jp.co.panpanini.Unmarshaller
import kotlin.Any
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.Map
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class Item() : Message<Item>, Serializable {
    var id: String = ""
        private set
    var unknownFields: Map<Int, UnknownField> = emptyMap()
        private set
    override val protoSize: Int
        get() = protoSizeImpl()

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

    fun Item.protoMergeImpl(other: Item?): Item = other?.copy {
        unknownFields = unknownFields + other.unknownFields
    } ?: this

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: Item?): Item = protoMergeImpl(other)

    fun copy(block: Builder.() -> Unit): Item = newBuilder().apply {
        block(this)
    }
    .build()

    override fun equals(other: Any?): Boolean = other is Item &&
    id == other.id

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + unknownFields.hashCode()
        return result
    }

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
                    0 -> return Builder()
                            .id(id)
                            .unknownFields(protoUnmarshal.unknownFields())
                            .build()

                    10 -> id = protoUnmarshal.readString()
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): Item = protoUnmarshal(arr)

        fun with(block: Builder.() -> Unit) = Item().copy(block)
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

        fun build(): Item = Item().apply {
        id = this@Builder.id
        unknownFields = this@Builder.unknownFields
        }
    }
}
