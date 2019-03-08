package api

import jp.co.panpanini.Message
import pbandk.Marshaller
import pbandk.MessageMap
import pbandk.UnknownField
import pbandk.Unmarshaller
import java.io.Serializable

data class Mappy(
    @JvmField val id: String = "",
    @JvmField val things: Map<String, api.Thing> = emptyMap(),
    val unknownFields: Map<Int, UnknownField> = emptyMap()
) : Message<Mappy>, Serializable {
    override val protoSize: Int = protoSizeImpl()


    constructor(id: String, things: Map<String, api.Thing>) : this(id, things, emptyMap())

    fun Mappy.protoSizeImpl(): Int {
        var protoSize = 0
        if (id != DEFAULT_ID) {
            protoSize += pbandk.Sizer.tagSize(1) + pbandk.Sizer.stringSize(id)
        }
        if (things.isNotEmpty()) {
            protoSize += pbandk.Sizer.mapSize(2, things, api.Mappy::ThingsEntry)
        }
        protoSize += unknownFields.entries.sumBy {
                 it.value.size()
                 }
        return protoSize
    }

    fun Mappy.protoMarshalImpl(protoMarshal: Marshaller) {
        if (id != DEFAULT_ID) {
            protoMarshal.writeTag(10).writeString(id)

        }
        if (things.isNotEmpty()) {
            protoMarshal.writeMap(18, things, api.Mappy::ThingsEntry)

        }
        if (unknownFields.isNotEmpty()) {
            protoMarshal.writeUnknownFields(unknownFields)
        }
    }

    fun Mappy.protoMergeImpl(other: Mappy?): Mappy {
        val obj = other?.copy(
        things = things + other.things,
        unknownFields = unknownFields + other.unknownFields
        ) ?: this
        return obj
    }

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: Mappy?): Mappy = protoMergeImpl(other)

    fun encode(): ByteArray = protoMarshal()

    override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Mappy =
            Companion.protoUnmarshal(protoUnmarshal)
    fun newBuilder(): Builder {
        val builder =  Builder()
            .id(id)
            .things(things)
            .unknownFields(unknownFields)
        return builder
    }

    data class ThingsEntry(
        override val key: String,
        override val value: api.Thing,
        val unknownFields: Map<Int, UnknownField> = emptyMap()
    ) : Message<ThingsEntry>, Serializable, Map.Entry<String, api.Thing> {
        override val protoSize: Int = protoSizeImpl()


        constructor(key: String, value: api.Thing) : this(key, value, emptyMap())

        fun ThingsEntry.protoSizeImpl(): Int {
            var protoSize = 0
            if (key != DEFAULT_KEY) {
                protoSize += pbandk.Sizer.tagSize(1) + pbandk.Sizer.stringSize(key)
            }
            if (value != DEFAULT_VALUE) {
                protoSize += pbandk.Sizer.tagSize(2) + pbandk.Sizer.messageSize(value)
            }
            protoSize += unknownFields.entries.sumBy {
                     it.value.size()
                     }
            return protoSize
        }

        fun ThingsEntry.protoMarshalImpl(protoMarshal: Marshaller) {
            if (key != DEFAULT_KEY) {
                protoMarshal.writeTag(10).writeString(key)

            }
            if (value != DEFAULT_VALUE) {
                protoMarshal.writeTag(18).writeMessage(value)

            }
            if (unknownFields.isNotEmpty()) {
                protoMarshal.writeUnknownFields(unknownFields)
            }
        }

        fun ThingsEntry.protoMergeImpl(other: ThingsEntry?): ThingsEntry {
            val obj = other?.copy(
            value = value?.plus(other.value) ?: value,
            unknownFields = unknownFields + other.unknownFields
            ) ?: this
            return obj
        }

        override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

        override operator fun plus(other: ThingsEntry?): ThingsEntry = protoMergeImpl(other)

        fun encode(): ByteArray = protoMarshal()

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): ThingsEntry =
                Companion.protoUnmarshal(protoUnmarshal)
        companion object : Message.Companion<ThingsEntry> {
            @JvmField
            val DEFAULT_KEY: String = ""

            @JvmField
            val DEFAULT_VALUE: api.Thing = api.Thing()

            override fun protoUnmarshal(protoUnmarshal: Unmarshaller): ThingsEntry {
                var key = ""
                var value: api.Thing? = api.Thing()
                while (true) {
                    when (protoUnmarshal.readTag()) {
                        0 -> return ThingsEntry(
                                key!!, value!!, protoUnmarshal.unknownFields()
                                )
                        10 -> key = protoUnmarshal.readString()
                        18 -> value = protoUnmarshal.readMessage(api.Thing.Companion)
                        else -> protoUnmarshal.unknownField()
                    }
                }
            }

            @JvmStatic
            fun decode(arr: ByteArray): ThingsEntry = protoUnmarshal(arr)
        }
    }

    companion object : Message.Companion<Mappy> {
        @JvmField
        val DEFAULT_ID: String = ""

        @JvmField
        val DEFAULT_THINGS: Map<String, api.Thing> = emptyMap()

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Mappy {
            var id = ""
            var things: MessageMap.Builder<String, api.Thing>? = null
            while (true) {
                when (protoUnmarshal.readTag()) {
                    0 -> return Mappy(
                            id!!, pbandk.MessageMap.Builder.fixed(things)!!,
                                    protoUnmarshal.unknownFields()
                            )
                    10 -> id = protoUnmarshal.readString()
                    18 -> things = protoUnmarshal.readMap(things, api.Mappy.ThingsEntry.Companion,
                            true)
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): Mappy = protoUnmarshal(arr)
    }

    class Builder {
        var id: String = DEFAULT_ID

        var things: Map<String, api.Thing> = DEFAULT_THINGS

        var unknownFields: Map<Int, UnknownField> = emptyMap()

        fun id(id: String?): Builder {
            this.id = id ?: DEFAULT_ID
            return this
        }

        fun things(things: Map<String, api.Thing>?): Builder {
            this.things = things ?: DEFAULT_THINGS
            return this
        }

        fun unknownFields(unknownFields: Map<Int, UnknownField>): Builder {
            this.unknownFields = unknownFields
            return this
        }

        fun build(): Mappy {
            val obj = Mappy(id, things, unknownFields)
            return obj
        }
    }
}
