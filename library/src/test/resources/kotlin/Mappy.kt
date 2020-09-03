// Code generated by protok protocol buffer plugin, do not edit.
// Source file: map.proto
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

class Mappy() : Message<Mappy>, Serializable {
    var id: String = ""
        private set
    var things: Map<String, api.Thing> = emptyMap()
        private set
    var unknownFields: Map<Int, UnknownField> = emptyMap()
        private set
    override val protoSize: Int
        get() = protoSizeImpl()

    fun Mappy.protoSizeImpl(): Int {
        var protoSize = 0
        if (id != DEFAULT_ID) {
            protoSize += jp.co.panpanini.Sizer.tagSize(1) + jp.co.panpanini.Sizer.stringSize(id)
        }
        if (things.isNotEmpty()) {
            protoSize += jp.co.panpanini.Sizer.mapSize(2, things) { key, value ->
                    api.Mappy.ThingsEntry.Builder().apply {
                        key(key)
                        value(value)
                    }
                    .build()
                    }
        }
        protoSize += unknownFields.entries.sumBy { it.value.size() }
        return protoSize
    }

    fun Mappy.protoMarshalImpl(protoMarshal: Marshaller) {
        if (id != DEFAULT_ID) {
            protoMarshal.writeTag(10).writeString(id)

        }
        if (things.isNotEmpty()) {
            protoMarshal.writeMap(18, things) { key, value ->
                    api.Mappy.ThingsEntry.Builder().apply {
                        key(key)
                        value(value)
                    }
                    .build()
                    }
        }
        if (unknownFields.isNotEmpty()) {
            protoMarshal.writeUnknownFields(unknownFields)
        }
    }

    fun Mappy.protoMergeImpl(other: Mappy?): Mappy = other?.copy {
        things = things + other.things
        unknownFields = unknownFields + other.unknownFields
    } ?: this

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: Mappy?): Mappy = protoMergeImpl(other)

    fun copy(block: Builder.() -> Unit): Mappy = newBuilder().apply {
        block(this)
    }
    .build()

    override fun equals(other: Any?): Boolean = other is Mappy &&
    id == other.id &&
    things == other.things

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + things.hashCode()
        result = 31 * result + unknownFields.hashCode()
        return result
    }

    fun encode(): ByteArray = protoMarshal()

    override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Mappy =
            Companion.protoUnmarshal(protoUnmarshal)

    fun newBuilder(): Builder = Builder()
        .id(id)
        .things(things)
        .unknownFields(unknownFields)

    class ThingsEntry() : Message<ThingsEntry>, Serializable, Map.Entry<String, api.Thing> {
        override var key: String = ""
            private set
        override var value: api.Thing = api.Thing()
            private set
        var unknownFields: Map<Int, UnknownField> = emptyMap()
            private set
        override val protoSize: Int
            get() = protoSizeImpl()

        fun ThingsEntry.protoSizeImpl(): Int {
            var protoSize = 0
            if (key != DEFAULT_KEY) {
                protoSize += jp.co.panpanini.Sizer.tagSize(1) +
                        jp.co.panpanini.Sizer.stringSize(key)
            }
            if (value != DEFAULT_VALUE) {
                protoSize += jp.co.panpanini.Sizer.tagSize(2) +
                        jp.co.panpanini.Sizer.messageSize(value)
            }
            protoSize += unknownFields.entries.sumBy { it.value.size() }
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

        fun ThingsEntry.protoMergeImpl(other: ThingsEntry?): ThingsEntry = other?.copy {
            value = value?.plus(other.value) ?: value
            unknownFields = unknownFields + other.unknownFields
        } ?: this

        override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

        override operator fun plus(other: ThingsEntry?): ThingsEntry = protoMergeImpl(other)

        fun copy(block: Builder.() -> Unit): ThingsEntry = newBuilder().apply {
            block(this)
        }
        .build()

        override fun equals(other: Any?): Boolean = other is ThingsEntry &&
        key == other.key &&
        value == other.value

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.hashCode()
            result = 31 * result + unknownFields.hashCode()
            return result
        }

        fun encode(): ByteArray = protoMarshal()

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): ThingsEntry =
                Companion.protoUnmarshal(protoUnmarshal)

        fun newBuilder(): Builder = Builder()
            .key(key)
            .value(value)
            .unknownFields(unknownFields)

        companion object : Message.Companion<ThingsEntry> {
            @JvmField
            val DEFAULT_KEY: String = ""

            @JvmField
            val DEFAULT_VALUE: api.Thing = api.Thing()

            override fun protoUnmarshal(protoUnmarshal: Unmarshaller): ThingsEntry {
                var key = ""
                var value: api.Thing = api.Thing()
                while (true) {
                    when (protoUnmarshal.readTag()) {
                        0 -> return Builder()
                                .key(key)
                                .value(value)
                                .unknownFields(protoUnmarshal.unknownFields())
                                .build()

                        10 -> key = protoUnmarshal.readString()
                        18 -> value = protoUnmarshal.readMessage(api.Thing.Companion)
                        else -> protoUnmarshal.unknownField()
                    }
                }
            }

            @JvmStatic
            fun decode(arr: ByteArray): ThingsEntry = protoUnmarshal(arr)

            fun with(block: Builder.() -> Unit) = ThingsEntry().copy(block)
        }

        class Builder {
            var key: String = DEFAULT_KEY

            var value: api.Thing = DEFAULT_VALUE

            var unknownFields: Map<Int, UnknownField> = emptyMap()

            fun key(key: String?): Builder {
                this.key = key ?: DEFAULT_KEY
                return this
            }

            fun value(value: api.Thing?): Builder {
                this.value = value ?: DEFAULT_VALUE
                return this
            }

            fun unknownFields(unknownFields: Map<Int, UnknownField>): Builder {
                this.unknownFields = unknownFields
                return this
            }

            fun build(): ThingsEntry = ThingsEntry().apply {
            key = this@Builder.key
            value = this@Builder.value
            unknownFields = this@Builder.unknownFields
            }
        }
    }

    companion object : Message.Companion<Mappy> {
        @JvmField
        val DEFAULT_ID: String = ""

        @JvmField
        val DEFAULT_THINGS: Map<String, api.Thing> = emptyMap()

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): Mappy {
            var id = ""
            var things: Map<String, api.Thing> = emptyMap()
            while (true) {
                when (protoUnmarshal.readTag()) {
                    0 -> return Builder()
                            .id(id)
                            .things(HashMap(things))
                            .unknownFields(protoUnmarshal.unknownFields())
                            .build()

                    10 -> id = protoUnmarshal.readString()
                    18 -> things = protoUnmarshal.readMap(things, api.Mappy.ThingsEntry.Companion,
                            true)
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): Mappy = protoUnmarshal(arr)

        fun with(block: Builder.() -> Unit) = Mappy().copy(block)
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

        fun build(): Mappy = Mappy().apply {
        id = this@Builder.id
        things = this@Builder.things
        unknownFields = this@Builder.unknownFields
        }
    }
}
