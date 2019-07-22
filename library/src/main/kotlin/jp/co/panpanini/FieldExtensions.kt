package jp.co.panpanini

import com.improve_future.case_changer.toSnakeCase
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import pbandk.gen.File

fun File.Field.Standard.unmarshalReadExpression(kotlinTypeMappings: Map<String, String>) = type.neverPacked.let { neverPacked ->
    val repEnd = if (neverPacked) "true," else "false,"
    when (type) {
        File.Field.Type.ENUM ->
            if (repeated) "protoUnmarshal.readRepeatedEnum($kotlinFieldName, ${kotlinQualifiedTypeName(kotlinTypeMappings)}.Companion)"
            else "protoUnmarshal.readEnum(${kotlinQualifiedTypeName(kotlinTypeMappings)}.Companion)"
        File.Field.Type.MESSAGE ->
            if (!repeated) "protoUnmarshal.readMessage(${kotlinQualifiedTypeName(kotlinTypeMappings)}.Companion)"
            else if (map) "protoUnmarshal.readMap($kotlinFieldName, ${kotlinQualifiedTypeName(kotlinTypeMappings)}.Companion$repEnd)"
            else "protoUnmarshal.readRepeatedMessage($kotlinFieldName, ${kotlinQualifiedTypeName(kotlinTypeMappings)}.Companion$repEnd)"
        else -> {
            if (repeated) "protoUnmarshal.readRepeated($kotlinFieldName, $repEnd protoUnmarshal::${type.readMethod})"
            else "protoUnmarshal.${type.readMethod}()"
        }
    }
}

fun File.Field.Standard.kotlinQualifiedTypeName(kotlinTypeMappings: Map<String, String>): TypeName = when {
    kotlinLocalTypeName != null -> ClassName("", kotlinLocalTypeName!!)
    localTypeName != null -> ClassName("", kotlinTypeMappings.getOrElse(localTypeName!!) { error("Unable to find mapping for $localTypeName") })
    else -> type.standardTypeName
}

val File.Field.defaultValueName : String
    get() = "DEFAULT_${this.kotlinFieldName.capitalize().toSnakeCase().toUpperCase()}"

val File.Field.Standard.tag get() = (number shl 3) or when {
    repeated && packed -> 2
    else -> type.wireFormat
}

fun File.Field.Standard.defaultValue(version: Int, kotlinTypeMappings: Map<String, String>) = when {
    map -> "emptyMap()"
    repeated -> "emptyList()"
    version == 2 && optional -> "null"
    type == File.Field.Type.ENUM -> "${kotlinQualifiedTypeName(kotlinTypeMappings)}.fromValue(0)"
    type == File.Field.Type.MESSAGE -> "${kotlinQualifiedTypeName(kotlinTypeMappings)}()"
    else -> type.defaultValue
}

fun File.Field.Standard.localType(file: File) = localTypeName?.let { findLocalType(file, it) }

private fun findLocalType(file: File, protoName: String, parent: File.Type.Message? = null): File.Type? {
    // Get the set to look in and the type name
    val (lookIn, typeName) =
            if (parent == null) file.types to protoName.removePrefix(".${file.packageName}.")
            else parent.nestedTypes to protoName
    // Go deeper if there's a dot
    typeName.indexOf('.').let { period ->
        if (period == -1) return lookIn.find { it.name == typeName }
        return findLocalType(file, typeName.substring(period + 1), typeName.substring(0, period).let { parentTypeName ->
            lookIn.find { it.name == parentTypeName } as? File.Type.Message
        } ?: return null)
    }
}

val File.Field.OneOf.type get() = ClassName("", kotlinTypeName)

val File.Field.OneOf.defaultValue get() = "$kotlinTypeName.NotSet"

