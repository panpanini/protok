package jp.co.panpanini

import com.google.protobuf.CodedOutputStream

actual fun computeUInt64Size(value: Long) = CodedOutputStream.computeUInt64SizeNoTag(value)

actual fun computeStringSize(value: String) = CodedOutputStream.computeStringSizeNoTag(value)