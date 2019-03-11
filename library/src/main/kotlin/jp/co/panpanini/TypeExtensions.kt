package jp.co.panpanini

import com.squareup.kotlinpoet.asTypeName
import pbandk.gen.File

val File.Field.Type.readMethod get() = "read" + string.capitalize()

val File.Field.Type.string get() = when (this) {
    File.Field.Type.BOOL -> "bool"
    File.Field.Type.BYTES -> "bytes"
    File.Field.Type.DOUBLE -> "double"
    File.Field.Type.ENUM -> "enum"
    File.Field.Type.FIXED32 -> "fixed32"
    File.Field.Type.FIXED64 -> "fixed64"
    File.Field.Type.FLOAT -> "float"
    File.Field.Type.INT32 -> "int32"
    File.Field.Type.INT64 -> "int64"
    File.Field.Type.MESSAGE -> "message"
    File.Field.Type.SFIXED32 -> "sFixed32"
    File.Field.Type.SFIXED64 -> "sFixed64"
    File.Field.Type.SINT32 -> "sInt32"
    File.Field.Type.SINT64 -> "sInt64"
    File.Field.Type.STRING -> "string"
    File.Field.Type.UINT32 -> "uInt32"
    File.Field.Type.UINT64 -> "uInt64"
}

val File.Field.Type.wireFormat get() = when (this) {
    File.Field.Type.BOOL, File.Field.Type.ENUM, File.Field.Type.INT32, File.Field.Type.INT64,
    File.Field.Type.SINT32, File.Field.Type.SINT64, File.Field.Type.UINT32, File.Field.Type.UINT64 -> 0
    File.Field.Type.BYTES, File.Field.Type.MESSAGE, File.Field.Type.STRING -> 2
    File.Field.Type.DOUBLE, File.Field.Type.FIXED64, File.Field.Type.SFIXED64 -> 1
    File.Field.Type.FIXED32, File.Field.Type.FLOAT, File.Field.Type.SFIXED32 -> 5
}

val File.Field.Type.standardTypeName get() = when (this) {
    File.Field.Type.BOOL -> Boolean::class.asTypeName()
    File.Field.Type.BYTES -> pbandk.ByteArr::class.asTypeName()
    File.Field.Type.DOUBLE -> Double::class.asTypeName()
    File.Field.Type.ENUM -> error("No standard type name for enums")
    File.Field.Type.FIXED32 -> Int::class.asTypeName()
    File.Field.Type.FIXED64 -> Long::class.asTypeName()
    File.Field.Type.FLOAT -> Float::class.asTypeName()
    File.Field.Type.INT32 -> Int::class.asTypeName()
    File.Field.Type.INT64 -> Long::class.asTypeName()
    File.Field.Type.MESSAGE -> error("No standard type name for messages")
    File.Field.Type.SFIXED32 -> Int::class.asTypeName()
    File.Field.Type.SFIXED64 -> Long::class.asTypeName()
    File.Field.Type.SINT32 -> Int::class.asTypeName()
    File.Field.Type.SINT64 -> Long::class.asTypeName()
    File.Field.Type.STRING -> String::class.asTypeName()
    File.Field.Type.UINT32 -> Int::class.asTypeName()
    File.Field.Type.UINT64 -> Long::class.asTypeName()
}

val File.Field.Type.defaultValue get() = when (this) {
    File.Field.Type.BOOL -> "false"
    File.Field.Type.BYTES -> "pbandk.ByteArr.empty"
    File.Field.Type.DOUBLE -> "0.0"
    File.Field.Type.ENUM -> error("No generic default value for enums")
    File.Field.Type.FIXED32, File.Field.Type.INT32, File.Field.Type.SFIXED32,
    File.Field.Type.SINT32, File.Field.Type.UINT32 -> "0"
    File.Field.Type.FIXED64, File.Field.Type.INT64, File.Field.Type.SFIXED64,
    File.Field.Type.SINT64, File.Field.Type.UINT64 -> "0L"
    File.Field.Type.FLOAT -> "0.0F"
    File.Field.Type.MESSAGE -> "null"
    File.Field.Type.STRING -> "\"\""
}

val pbandk.gen.File.Field.Type.writeMethod get() = "write" + string.capitalize()

val File.Field.Type.sizeMethod get() = string + "Size"

val File.Field.Type.requiresExplicitTypeWithVal get() =
    this == File.Field.Type.BYTES || this == File.Field.Type.ENUM || this == File.Field.Type.MESSAGE
