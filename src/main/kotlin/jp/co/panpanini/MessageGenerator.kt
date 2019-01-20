package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import pbandk.Message
import pbandk.UnknownField
import pbandk.gen.File

class MessageGenerator(private val file: File, private val kotlinTypeMappings: Map<String, String>) {


    fun buildMessage(type: File.Type.Message): TypeSpec {
        val className = ClassName("", type.kotlinTypeName)
        val mapEntry = if (type.mapEntry) {
            val keyName = type.mapEntryKeyKotlinType!!
            val valueName = type.mapEntryValueKotlinType!!
            Map.Entry::class.asClassName().parameterizedBy(keyName, valueName)
        } else {
            null
        }
        val superInterface = Message::class.asClassName().parameterizedBy(className)
        val typeSpec = TypeSpec.classBuilder(type.kotlinTypeName)
                .addSuperinterface(superInterface)
                .addModifiers(KModifier.DATA)

        val constructor = FunSpec.constructorBuilder()

        type.fields.mapIndexed { index, field ->
            val param = when (field) {
                is File.Field.Standard -> PropertySpec.builder(field.kotlinFieldName, field.kotlinValueType(true)).initializer(field.kotlinFieldName)

                is File.Field.OneOf -> TODO()
            }
            if (index == 0 && type.mapEntry) {
                // add override to first item of map entry
                param.addModifiers(KModifier.OVERRIDE)
            }
            param.build()
        }.forEach {
            constructor.addParameter(it.name, it.type)
            typeSpec.addProperty(it)
        }
        // unknown fields
       val unknownPropertySpec = unknownFieldSpec()
        constructor.addParameter(unknownPropertySpec.name, unknownPropertySpec.type)
        typeSpec.addProperty(unknownPropertySpec)

        typeSpec.primaryConstructor(constructor.build())

        mapEntry?.let {
            typeSpec.addSuperinterface(mapEntry)
        }

        type.nestedTypes.map {
            it.toTypeSpec(file, kotlinTypeMappings)
        }.forEach {
            typeSpec.addType(it)
        }
        typeSpec.addFunction(createMessageSizeExtension(type, className))
        return typeSpec.build()
    }


    private fun unknownFieldSpec(): PropertySpec {
        return PropertySpec.builder(
                "unknownFields" ,
                Map::class.parameterizedBy(Int::class, UnknownField::class)
        )
                .initializer("unknownFields")
                .build()
    }

    private fun createMessageSizeExtension(type: File.Type.Message, typeName: ClassName): FunSpec {
        val funSpec = FunSpec.builder("protoSizeImpl")
                .returns(Int::class)
                .receiver(typeName)

        val codeBlock = CodeBlock.builder()
                .addStatement("var protoSize = 0")
        type.fields.map {
            val fieldBlock = CodeBlock.builder()
            when(it) {
                is File.Field.Standard -> {
                    fieldBlock
                            .beginControlFlow("if (${it.getNonDefaultCheck()})")
                            .add("protoSize += ")
                            .addStatement(it.sizeExpression().toString())
                            .endControlFlow()
                }
            }

            fieldBlock.build()
        }.forEach {
            codeBlock.add(it)
        }
        // unknownFields
        codeBlock.addStatement("protoSize += unknownFields.entries.sumBy { it.value.size() }")

        codeBlock.addStatement("return protoSize")

        funSpec.addCode(codeBlock.build())

        return funSpec.build()
    }

    private fun File.Field.Standard.sizeExpression(): CodeBlock {
        val sizer = ClassName("pbandk", "Sizer")
        val codeBlock = CodeBlock.builder()

        when {
            map -> {
                codeBlock.add("%T.mapSize($number, $kotlinFieldName, ${mapConstructorReference()})", sizer)
            }
            repeated && packed -> {
                codeBlock.add("%T.tagSize($number) + %T.packedRepeatedSize($kotlinFieldName, %T::${type.sizeMethod}", sizer, sizer, sizer)
            }
            repeated -> {
                codeBlock.add("%T.tagSize($number) * $kotlinFieldName.size + $kotlinFieldName.sumBy(%T::${type.sizeMethod})", sizer, sizer)
            }
            else -> {
                codeBlock.add("%T.tagSize($number) + %T.${type.sizeMethod}($kotlinFieldName)", sizer, sizer)
            }


        }
        return codeBlock.build()
    }

    private val File.Field.Type.string get() = when (this) {
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

    private val File.Field.Type.sizeMethod get() = string + "Size"

    private fun File.Field.Standard.mapConstructorReference(): CodeBlock {
        return CodeBlock.of(
                kotlinQualifiedTypeName.let { it ->
                    val type = it.toString()
                    type.lastIndexOf('.').let { if (it == -1) "::$type" else type.substring(0, it) + "::" + type.substring(it + 1) }
                }
        )
    }

    private fun File.Field.Standard.getNonDefaultCheck(): CodeBlock {
        return when {
            repeated -> CodeBlock.of("$kotlinFieldName.isNotEmpty()")
            file.version == 2 && optional -> CodeBlock.of("$kotlinFieldName != null")
            else -> type.getNonDefaultCheck(kotlinFieldName)
        }
    }

    private fun File.Field.Type.getNonDefaultCheck(fieldName: String): CodeBlock {
        return when(this) {
            File.Field.Type.BOOL -> CodeBlock.of(fieldName)
            File.Field.Type.BYTES -> CodeBlock.of("$fieldName.array.isNotEmpty()")
            File.Field.Type.ENUM -> CodeBlock.of("$fieldName.value != 0")
            File.Field.Type.STRING -> CodeBlock.of("$fieldName.isNotEmpty()")
            else -> CodeBlock.of("$fieldName != $defaultValue")
        }
    }

    private val File.Field.Type.defaultValue get() = when (this) {
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

    private val File.Type.Message.mapEntryKeyKotlinType get() =
        if (!mapEntry) null else (fields[0] as File.Field.Standard).kotlinValueType(false)
    private val File.Type.Message.mapEntryValueKotlinType get() =
        if (!mapEntry) null else (fields[1] as File.Field.Standard).kotlinValueType(true)

    private fun File.Field.Standard.mapEntry() =
            if (!map) null else (localType as? File.Type.Message)?.takeIf { it.mapEntry }

    private val File.Field.Standard.localType get() = localTypeName?.let { findLocalType(it) }


    private fun File.Field.Standard.kotlinValueType(nullableIfMessage: Boolean): TypeName = when {
        map -> mapEntry()!!.let {
            val key = it.mapEntryKeyKotlinType ?: return@let kotlinQualifiedTypeName
            val value = it.mapEntryValueKotlinType ?: return@let kotlinQualifiedTypeName

            val param = Map::class.asTypeName().parameterizedBy(key, value)

            param
        }
        repeated -> List::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName)
        (file.version == 2 && optional) || (type == File.Field.Type.MESSAGE && nullableIfMessage) ->
            kotlinQualifiedTypeName.copy(nullable = true)
        else -> kotlinQualifiedTypeName
    }

    private val File.Field.Standard.kotlinQualifiedTypeName: TypeName
        get() {
            return when {
                kotlinLocalTypeName != null -> ClassName("", kotlinLocalTypeName!!)
                localTypeName != null -> ClassName("", kotlinTypeMappings.getOrElse(localTypeName!!) { error("Unable to find mapping for $localTypeName") })
                else -> type.standardTypeName
            }
        }

    private fun findLocalType(protoName: String, parent: File.Type.Message? = null): File.Type? {
        // Get the set to look in and the type name
        val (lookIn, typeName) =
                if (parent == null) file.types to protoName.removePrefix(".${file.packageName}.")
                else parent.nestedTypes to protoName
        // Go deeper if there's a dot
        typeName.indexOf('.').let { period ->
            if (period == -1) return lookIn.find { it.name == typeName }
            return findLocalType(typeName.substring(period + 1), typeName.substring(0, period).let { parentTypeName ->
                lookIn.find { it.name == parentTypeName } as? File.Type.Message
            } ?: return null)
        }
    }

    private val File.Field.Type.standardTypeName get() = when (this) {
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
}