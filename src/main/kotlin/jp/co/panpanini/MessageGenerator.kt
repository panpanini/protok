package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import pbandk.*
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
            if (type.mapEntry) {
                // add override map entry
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
        typeSpec.addFunction(createMessageMarshalExtension(type, className))
        typeSpec.addFunction(createMessageMergeExtension(type, className))
        typeSpec.addProperty(createProtoSizeVal())
        typeSpec.addFunction(createProtoMarshalFunction())
        typeSpec.addFunction(createPlusOperator(className))
        typeSpec.addType(createCompanionObject(type, className))
        return typeSpec.build()
    }

    private fun createProtoSizeVal(): PropertySpec {
        return PropertySpec.builder("protoSize", Int::class)
                .addModifiers(KModifier.OVERRIDE)
                .delegate(CodeBlock.builder()
                        .beginControlFlow("lazy")
                        .addStatement("protoSizeImpl()")
                        .endControlFlow()
                        .build()
                ).build()
    }

    private fun createProtoMarshalFunction(): FunSpec {
        return FunSpec.builder("protoMarshal")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("marshaller", Marshaller::class)
                .addCode(CodeBlock.builder()
                        .addStatement("return protoMarshalImpl(marshaller)")
                        .build()
                )
                .build()
    }

    private fun createPlusOperator(typeName: ClassName): FunSpec {
        return FunSpec.builder("plus")
                .addParameter("other", typeName.copy(nullable = true))
                .returns(typeName)
                .addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
                .addCode(CodeBlock.builder()
                        .addStatement("return protoMergeImpl(other)")
                        .build()
                )
                .build()
    }

    private fun createCompanionObject(type: File.Type.Message, typeName: ClassName): TypeSpec {
        val companion = TypeSpec.companionObjectBuilder()
                .addSuperinterface(Message.Companion::class.asClassName().parameterizedBy(typeName))
                .addFunction(createProtoUnmarshalFunction(type, typeName))

        return companion.build()
    }

    private fun unknownFieldSpec(): PropertySpec {
        return PropertySpec.builder(
                "unknownFields" ,
                Map::class.parameterizedBy(Int::class, UnknownField::class)
        )
                .initializer("unknownFields")
                .build()
    }

    private fun File.Field.Standard.unmarshalLocalVar(): CodeBlock {
        val codeBlock = CodeBlock.builder()

        when {
            repeated -> {
                mapEntry().let {
                    if (it == null) {
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", ListWithSize.Builder::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName).copy(nullable = true))
                    } else {
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", MessageMap.Builder::class.asTypeName().parameterizedBy(it.mapEntryKeyKotlinType!!, it.mapEntryValueKotlinType!!).copy(nullable = true))
                    }
                }
            }
            requiresExplicitTypeWithVal -> {
                codeBlock.addStatement("var $kotlinFieldName: ${kotlinValueType(true)} = $defaultValue")
            }
            else -> {
                codeBlock.addStatement("var $kotlinFieldName = $defaultValue")
            }
        }
        return codeBlock.build()
    }

    private val File.Field.Standard.requiresExplicitTypeWithVal get() =
        repeated || (file.version == 2 && optional) || type.requiresExplicitTypeWithVal

    private val File.Field.Type.requiresExplicitTypeWithVal get() =
        this == File.Field.Type.BYTES || this == File.Field.Type.ENUM || this == File.Field.Type.MESSAGE

    //TODO
    private val File.Field.Standard.unmarshalVarDone get() =
        when {
            map -> "pbandk.MessageMap.Builder.fixed($kotlinFieldName)"
            repeated -> "pbandk.ListWithSize.Builder.fixed($kotlinFieldName)"
            else -> kotlinFieldName
        }

    private val File.Field.Standard.defaultValue get() = when {
        map -> "emptyMap()"
        repeated -> "emptyList()"
        file.version == 2 && optional -> "null"
        type == File.Field.Type.ENUM -> "$kotlinQualifiedTypeName.fromValue(0)"
        else -> type.defaultValue
    }

    private fun createMessageMergeExtension(type: File.Type.Message, typeName: ClassName): FunSpec {
        val codeBlock = CodeBlock.builder()
                .add("return ")
                .addStatement("other?.copy(")
                .indent()
        type.fields.mapNotNull {
            when (it) {
                is File.Field.Standard -> buildStandardFieldMerge(it)
                is File.Field.OneOf -> buildOneOfFieldMerge(it)
            }
        }.forEach {
            codeBlock.addStatement("$it,")
        }
        codeBlock.addStatement("unknownFields = unknownFields + other.unknownFields")
                .unindent()
                .addStatement(") ?: this")
        return FunSpec.builder("protoMergeImpl")
                .receiver(typeName)
                .returns(typeName)
                .addCode(codeBlock.build())
                .addParameter("other", typeName.copy(nullable = true))
                .build()
    }

    private fun buildStandardFieldMerge(field: File.Field.Standard): CodeBlock? {
        return when {
            field.repeated -> {
                CodeBlock.of("${field.kotlinFieldName} = ${field.kotlinFieldName} + other.${field.kotlinFieldName}")
            }
            field.type == File.Field.Type.MESSAGE -> {
                CodeBlock.of("${field.kotlinFieldName} = ${field.kotlinFieldName}?.plus(other.${field.kotlinFieldName}) ?: ${field.kotlinFieldName}")
            }
            file.version == 2 && field.optional -> {
                CodeBlock.of("${field.kotlinFieldName} = other?.${field.kotlinFieldName} : ${field.kotlinFieldName}")
            }
            else -> null
        }
    }

    private fun buildOneOfFieldMerge(field: File.Field.OneOf): CodeBlock {
        return TODO()
    }


    private fun createProtoUnmarshalFunction(type: File.Type.Message, typeName: ClassName): FunSpec {
        val unMarshalParameter = ParameterSpec.builder("protoUnmarshal", Unmarshaller::class).build()
        val funSpec = FunSpec.builder("protoUnmarshal")
                .addModifiers(KModifier.OVERRIDE)
                .returns(typeName)
                .addParameter(unMarshalParameter)

        // local variables
        val codeBlock = CodeBlock.builder()

        val doneKotlinFields = type.fields.map {
            when (it) {
                is File.Field.Standard -> {
                    Pair(it.unmarshalLocalVar(), it.unmarshalVarDone)
                }
                is File.Field.OneOf -> {
                    Pair(CodeBlock.builder()
                            .addStatement("var ${it.kotlinTypeName}: $typeName.${it.kotlinTypeName}? = null")
                            .build(),
                            it.kotlinFieldName)
                }
            }
        }.map { (code, unmarshalVar) ->
            codeBlock.add(code)
            unmarshalVar
        }

        //TODO: clean this up - its a little difficult to follow. maybe create a function for it
        codeBlock.beginControlFlow("while (true)")
        codeBlock.beginControlFlow("when (${unMarshalParameter.name}.readTag())")
                .addStatement("0 -> ${typeName.simpleName}(${doneKotlinFields.joinToString()}, ${unMarshalParameter.name}.unknownFields())")

        type.sortedStandardFieldsWithOneOfs().map { (field, oneOf) ->
            val tags = mutableListOf(field.tag)
            val fieldBlock = CodeBlock.builder()
            if (field.repeated) {
                val tag = (field.number shl 3) or if (field.packed) field.type.wireFormat else 2
                if (field.tag != tag) {
                    tags.add(tag)
                }
            }
                fieldBlock.add("${tags.joinToString()} -> ")
                if (oneOf == null) {
                    fieldBlock.addStatement("${field.kotlinFieldName} = ${field.unmarshalReadExpression}")
                } else {
                    val oneOfType = "${typeName.simpleName}.${oneOf.kotlinTypeName}.${oneOf.kotlinFieldTypeNames[field.name]}"
                    require(!field.repeated)
                    fieldBlock.addStatement("${oneOf.kotlinFieldName} = $oneOfType(${field.unmarshalReadExpression})")
                }

            fieldBlock.build()
        }.forEach {
            codeBlock.add(it)
        }

        codeBlock.addStatement(
                "else -> ${unMarshalParameter.name}.unknownField()"
        )
        codeBlock.endControlFlow() // when
        codeBlock.endControlFlow() // while

        funSpec.addCode(codeBlock.build())
        return funSpec.build()
    }

    protected val File.Field.Standard.unmarshalReadExpression get() = type.neverPacked.let { neverPacked ->
        val repEnd = if (neverPacked) ", true" else ", false"
        when (type) {
            File.Field.Type.ENUM ->
                if (repeated) "protoUnmarshal.readRepeatedEnum($kotlinFieldName, $kotlinQualifiedTypeName.Companion)"
                else "protoUnmarshal.readEnum($kotlinQualifiedTypeName.Companion)"
            File.Field.Type.MESSAGE ->
                if (!repeated) "protoUnmarshal.readMessage($kotlinQualifiedTypeName.Companion)"
                else if (map) "protoUnmarshal.readMap($kotlinFieldName, $kotlinQualifiedTypeName.Companion$repEnd)"
                else "protoUnmarshal.readRepeatedMessage($kotlinFieldName, $kotlinQualifiedTypeName.Companion$repEnd)"
            else -> {
                if (repeated) "protoUnmarshal.readRepeated($kotlinFieldName, protoUnmarshal::${type.readMethod}$repEnd)"
                else "protoUnmarshal.${type.readMethod}()"
            }
        }
    }

    protected val File.Field.Type.readMethod get() = "read" + string.capitalize()


    private fun createMessageMarshalExtension(type: File.Type.Message, typeName: ClassName): FunSpec {
        val marshalParameter = ParameterSpec.builder("protoMarshal", Marshaller::class).build()
        val funSpec = FunSpec.builder("protoMarshalImpl")
                .receiver(typeName)
                .addParameter(marshalParameter)

        // write all non-default fields
        val codeBlock = CodeBlock.builder()
        type.sortedStandardFieldsWithOneOfs()
                .map { (field, oneOf) ->
                    if (oneOf == null) {
                        // standard field
                        CodeBlock.builder()
                                .beginControlFlow("if (${field.getNonDefaultCheck()})")
                                .addStatement(field.writeExpressionToMarshaller(marshalParameter.name))
                                .endControlFlow()
                                .build()
                    } else {
                        val subclassName = "${typeName.canonicalName}.${oneOf.kotlinTypeName}.${oneOf.kotlinFieldTypeNames[field.name]}"

                        CodeBlock.builder()
                                .beginControlFlow("if (%s is %s)", oneOf.kotlinFieldName, subclassName)
                                .addStatement(field.writeExpressionToMarshaller(marshalParameter.name, "${oneOf.kotlinFieldName}.${field.kotlinFieldName}"))
                                .endControlFlow()
                                .build()

                    }
                }
                .forEach { codeBlock.add(it) }
        // unknownFields
        codeBlock.beginControlFlow("if (unknownFields.isNotEmpty())")
                .addStatement("${marshalParameter.name}.writeUnknownFields(unknownFields)")
                .endControlFlow()

        funSpec.addCode(codeBlock.build())

        return funSpec.build()
    }

    private fun File.Field.Standard.writeExpressionToMarshaller(marshaller: String, reference: String = kotlinFieldName): String {
        val codeBlock = CodeBlock.builder()
        when {
            map -> {
                codeBlock.addStatement("$marshaller.writeMap($tag, $reference, ${mapConstructorReference()})")
            }
            repeated && packed -> {
                codeBlock.addStatement("$marshaller.writeTag($tag).writePackedRepeated($reference, %T::${type.sizeMethod}, $marshaller::${type.writeMethod})", Sizer::class)
            }
            repeated -> {
                codeBlock.addStatement("$reference.forEach { $marshaller.writeTag($tag).${type.writeMethod}(it) }")
            }
            else -> {
                codeBlock.addStatement("$marshaller.writeTag($tag).${type.writeMethod}($reference)")
            }
        }
        return codeBlock.build().toString()
    }

    private fun File.Type.Message.sortedStandardFieldsWithOneOfs() =
            fields.flatMap {
                when (it) {
                    is File.Field.Standard -> listOf(it to null)
                    is File.Field.OneOf -> it.fields.map { f -> f to it }
                }
            }.sortedBy { (field, _) ->  field.number }

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

    protected val pbandk.gen.File.Field.Type.writeMethod get() = "write" + string.capitalize()


    private val File.Field.Standard.tag get() = (number shl 3) or when {
        repeated && packed -> 2
        else -> type.wireFormat
    }

    private val File.Field.Type.wireFormat get() = when (this) {
        File.Field.Type.BOOL, File.Field.Type.ENUM, File.Field.Type.INT32, File.Field.Type.INT64,
        File.Field.Type.SINT32, File.Field.Type.SINT64, File.Field.Type.UINT32, File.Field.Type.UINT64 -> 0
        File.Field.Type.BYTES, File.Field.Type.MESSAGE, File.Field.Type.STRING -> 2
        File.Field.Type.DOUBLE, File.Field.Type.FIXED64, File.Field.Type.SFIXED64 -> 1
        File.Field.Type.FIXED32, File.Field.Type.FLOAT, File.Field.Type.SFIXED32 -> 5
    }
}