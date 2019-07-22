// Code generated by protok protocol buffer plugin, do not edit.
// Source file: oneoftest.proto
package api

import java.io.Serializable
import jp.co.panpanini.Marshaller
import jp.co.panpanini.Message
import jp.co.panpanini.UnknownField
import jp.co.panpanini.Unmarshaller
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import pbandk.ByteArr

data class OneOfTest(@JvmField val oneofField: OneofField = OneofField.NotSet, val unknownFields:
        Map<Int, UnknownField> = emptyMap()) : Message<OneOfTest>, Serializable {
    override val protoSize: Int = protoSizeImpl()


    constructor(oneofField: OneofField) : this(oneofField, emptyMap())

    fun OneOfTest.protoSizeImpl(): Int {
        var protoSize = 0
        if (oneofField !is OneofField.NotSet) {
            protoSize += oneofField.run {
                when (this) {
                    is OneofField.OneofUint32 -> jp.co.panpanini.Sizer.tagSize(111) +
                            jp.co.panpanini.Sizer.uInt32Size(oneofUint32)
                    is OneofField.OneofString -> jp.co.panpanini.Sizer.tagSize(113) +
                            jp.co.panpanini.Sizer.stringSize(oneofString)
                    is OneofField.OneofBytes -> jp.co.panpanini.Sizer.tagSize(114) +
                            jp.co.panpanini.Sizer.bytesSize(oneofBytes)
                    is OneofField.OneofBool -> jp.co.panpanini.Sizer.tagSize(115) +
                            jp.co.panpanini.Sizer.boolSize(oneofBool)
                    is OneofField.OneofUint64 -> jp.co.panpanini.Sizer.tagSize(116) +
                            jp.co.panpanini.Sizer.uInt64Size(oneofUint64)
                    is OneofField.OneofFloat -> jp.co.panpanini.Sizer.tagSize(117) +
                            jp.co.panpanini.Sizer.floatSize(oneofFloat)
                    is OneofField.OneofDouble -> jp.co.panpanini.Sizer.tagSize(118) +
                            jp.co.panpanini.Sizer.doubleSize(oneofDouble)
                    is OneofField.OneofItem -> jp.co.panpanini.Sizer.tagSize(119) +
                            jp.co.panpanini.Sizer.messageSize(oneofItem)
                    else -> 0
                }
            }
        }
        protoSize += unknownFields.entries.sumBy { it.value.size() }
        return protoSize
    }

    fun OneOfTest.protoMarshalImpl(protoMarshal: Marshaller) {
        if (oneofField is OneOfTest.OneofField.OneofUint32) {
            protoMarshal.writeTag(888).writeUInt32(oneofField.oneofUint32)

        }
        if (oneofField is OneOfTest.OneofField.OneofString) {
            protoMarshal.writeTag(906).writeString(oneofField.oneofString)

        }
        if (oneofField is OneOfTest.OneofField.OneofBytes) {
            protoMarshal.writeTag(914).writeBytes(oneofField.oneofBytes)

        }
        if (oneofField is OneOfTest.OneofField.OneofBool) {
            protoMarshal.writeTag(920).writeBool(oneofField.oneofBool)

        }
        if (oneofField is OneOfTest.OneofField.OneofUint64) {
            protoMarshal.writeTag(928).writeUInt64(oneofField.oneofUint64)

        }
        if (oneofField is OneOfTest.OneofField.OneofFloat) {
            protoMarshal.writeTag(941).writeFloat(oneofField.oneofFloat)

        }
        if (oneofField is OneOfTest.OneofField.OneofDouble) {
            protoMarshal.writeTag(945).writeDouble(oneofField.oneofDouble)

        }
        if (oneofField is OneOfTest.OneofField.OneofItem) {
            protoMarshal.writeTag(954).writeMessage(oneofField.oneofItem)

        }
        if (unknownFields.isNotEmpty()) {
            protoMarshal.writeUnknownFields(unknownFields)
        }
    }

    fun OneOfTest.protoMergeImpl(other: OneOfTest?): OneOfTest = other?.copy(
        oneofField = when {
                    this.oneofField is OneofField.OneofItem && other.oneofField is
                        OneofField.OneofItem -> {
                        OneofField.OneofItem(this.oneofField.oneofItem + other.oneofField.oneofItem)
                    }
                    else -> {
                       
                        if (this.oneofField is OneofField.NotSet) other.oneofField else this.oneofField
                    }
                }
                ,
        unknownFields = unknownFields + other.unknownFields
    ) ?: this

    override fun protoMarshal(marshaller: Marshaller) = protoMarshalImpl(marshaller)

    override operator fun plus(other: OneOfTest?): OneOfTest = protoMergeImpl(other)

    fun encode(): ByteArray = protoMarshal()

    override fun protoUnmarshal(protoUnmarshal: Unmarshaller): OneOfTest =
            Companion.protoUnmarshal(protoUnmarshal)

    fun newBuilder(): Builder = Builder()
        .oneofField(oneofField)
        .unknownFields(unknownFields)

    sealed class OneofField {
        data class OneofUint32(val oneofUint32: Int = 0) : OneofField()

        data class OneofString(val oneofString: String = "") : OneofField()

        data class OneofBytes(val oneofBytes: ByteArr = pbandk.ByteArr.empty) : OneofField()

        data class OneofBool(val oneofBool: Boolean = false) : OneofField()

        data class OneofUint64(val oneofUint64: Long = 0L) : OneofField()

        data class OneofFloat(val oneofFloat: Float = 0.0F) : OneofField()

        data class OneofDouble(val oneofDouble: Double = 0.0) : OneofField()

        data class OneofItem(val oneofItem: api.Item = api.Item()) : OneofField()

        object NotSet : OneofField()
    }

    companion object : Message.Companion<OneOfTest> {
        @JvmField
        val DEFAULT_ONEOF_FIELD: OneofField = OneofField.NotSet

        override fun protoUnmarshal(protoUnmarshal: Unmarshaller): OneOfTest {
            var oneofField: OneOfTest.OneofField = OneOfTest.OneofField.NotSet
            while (true) {
                when (protoUnmarshal.readTag()) {
                    0 -> return OneOfTest(oneofField, protoUnmarshal.unknownFields())
                    888 -> oneofField =
                            OneOfTest.OneofField.OneofUint32(protoUnmarshal.readUInt32())
                    906 -> oneofField =
                            OneOfTest.OneofField.OneofString(protoUnmarshal.readString())
                    914 -> oneofField = OneOfTest.OneofField.OneofBytes(protoUnmarshal.readBytes())
                    920 -> oneofField = OneOfTest.OneofField.OneofBool(protoUnmarshal.readBool())
                    928 -> oneofField =
                            OneOfTest.OneofField.OneofUint64(protoUnmarshal.readUInt64())
                    941 -> oneofField = OneOfTest.OneofField.OneofFloat(protoUnmarshal.readFloat())
                    945 -> oneofField =
                            OneOfTest.OneofField.OneofDouble(protoUnmarshal.readDouble())
                    954 -> oneofField =
                            OneOfTest.OneofField.OneofItem(protoUnmarshal.readMessage(api.Item.Companion))
                    else -> protoUnmarshal.unknownField()
                }
            }
        }

        @JvmStatic
        fun decode(arr: ByteArray): OneOfTest = protoUnmarshal(arr)
    }

    class Builder {
        var oneofField: OneofField = DEFAULT_ONEOF_FIELD

        var unknownFields: Map<Int, UnknownField> = emptyMap()

        fun oneofField(oneofField: OneofField?): Builder {
            this.oneofField = oneofField ?: DEFAULT_ONEOF_FIELD
            return this
        }

        fun unknownFields(unknownFields: Map<Int, UnknownField>): Builder {
            this.unknownFields = unknownFields
            return this
        }

        fun build(): OneOfTest = OneOfTest(oneofField, unknownFields)
    }
}
