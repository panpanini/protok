package jp.co.panpanini

import com.improve_future.case_changer.toSnakeCase
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import pbandk.*
import pbandk.gen.File
import java.io.Serializable

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
                .addSuperinterface(Serializable::class)
                .addModifiers(KModifier.DATA)

        val constructor = FunSpec.constructorBuilder()

        type.fields.map { field ->
            val param = when (field) {
                is File.Field.Standard -> PropertySpec.builder(field.kotlinFieldName, field.kotlinValueType(false)).initializer(field.kotlinFieldName)

                is File.Field.OneOf -> TODO()
            }
            if (type.mapEntry) {
                // add override map entry
                param.addModifiers(KModifier.OVERRIDE)
            } else {
                param.addAnnotation(JvmField::class)
            }
            Pair(param.build(), field)
        }.forEach { (property, field) ->
            val param = ParameterSpec.builder(property.name, property.type)
            if (!type.mapEntry) {
                param.defaultValue(field.defaultValue)
            }
            constructor.addParameter(param.build())
            typeSpec.addProperty(property)
        }
        // unknown fields
       val unknownPropertySpec = unknownFieldSpec()
        constructor.addParameter(ParameterSpec.builder(unknownPropertySpec.name, unknownPropertySpec.type).defaultValue("emptyMap()").build())
        typeSpec.addProperty(unknownPropertySpec)

        typeSpec.primaryConstructor(constructor.build())
        typeSpec.addFunction(createSecondaryConstructor(type))

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
        typeSpec.addFunction(createEncodeFunction())
        typeSpec.addFunction(createProtoUnmarshalFunction(className))
        if (!type.mapEntry) {
            typeSpec.addFunction(createNewBuilder(type, className))
            typeSpec.addType(createBuilder(type, className))
        }
        return typeSpec.build()
    }

    private fun createSecondaryConstructor(type: File.Type.Message): FunSpec {
        val constructor = FunSpec.constructorBuilder()
        val params = type.fields.map { field ->
            val param = when (field) {
                is File.Field.Standard -> ParameterSpec.builder(field.kotlinFieldName, field.kotlinValueType(false))

                is File.Field.OneOf -> TODO()
            }
            param.build()
        }

        params.forEach { param ->
            constructor.addParameter(param)
        }

        constructor.callThisConstructor(*params.map { it.name }.toTypedArray(), "emptyMap()")

        return constructor.build()
    }

    private fun createProtoSizeVal(): PropertySpec {
        return PropertySpec.builder("protoSize", Int::class)
                .addModifiers(KModifier.OVERRIDE)
                .initializer(CodeBlock.builder()
                        .addStatement("protoSizeImpl()")
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
                .addFunction(createCompanionProtoUnmarshalFunction(type, typeName))
                .addFunction(createDecodeFunction(type, typeName))

        createDefaultConstants(type)
                .forEach { companion.addProperty(it) }

        return companion.build()
    }

    private fun createNewBuilder(type: File.Type.Message, typeName: ClassName): FunSpec {
        val builder = ClassName("", "Builder")
        val funSpec = FunSpec.builder("newBuilder")
                .returns(builder)
        val codeBlock = CodeBlock.Builder()
                .addStatement("val builder =  Builder()")
                .indent()

        type.fields.map {
            when (it) {

                is File.Field.Standard -> {
                    ".${it.kotlinFieldName}(${it.kotlinFieldName})"
                }
                is File.Field.OneOf -> TODO()
            }
        }.forEach {
            codeBlock.addStatement(it)
        }

        //unknownFields
        codeBlock.addStatement(".unknownFields(unknownFields)")
                .unindent()

        // return builder
        codeBlock.addStatement("return builder")

        funSpec.addCode(codeBlock.build())
        return funSpec.build()
    }

    private fun createBuilder(type: File.Type.Message, typeName: ClassName): TypeSpec {
        val builder = ClassName("", "Builder")
        val typeSpec = TypeSpec.classBuilder(builder)
        type.fields.map {
            when (it) {
                is File.Field.Standard -> PropertySpec.builder(it.kotlinFieldName, it.kotlinValueType(false))
                        .mutable()
                        .initializer(it.defaultValue)
                        .build()
                is File.Field.OneOf -> TODO()
            }
        }.forEach {
            typeSpec.addProperty(it)
        }

        typeSpec.addProperty(PropertySpec.builder("unknownFields", Map::class.parameterizedBy(Int::class, UnknownField::class)).mutable().initializer("emptyMap()").build())

        type.fields.map {
            when (it) {
                is File.Field.Standard -> FunSpec.builder(it.kotlinFieldName)
                        .addParameter(ParameterSpec.builder(it.kotlinFieldName, it.kotlinValueType(true).copy(nullable = true)).build())
                        .returns(builder)
                        .addCode(CodeBlock.builder()
                                .addStatement("this.${it.kotlinFieldName} = ${it.kotlinFieldName} ?: ${it.defaultValue}")
                                .addStatement("return this")
                                .build()
                        )
                        .build()
                is File.Field.OneOf -> TODO()
            }
        }.forEach {
            typeSpec.addFunction(it)
        }
        typeSpec.addFunction(FunSpec.builder("unknownFields")
                .addParameter("unknownFields", Map::class.parameterizedBy(Int::class, UnknownField::class))
                .returns(builder)
                .addStatement("this.unknownFields = unknownFields")
                .addStatement("return this")
                .build())

        val code = CodeBlock.builder()
                .add("val obj = %T(", typeName)

        type.fields.map {
            when (it) {
                is File.Field.Standard -> {
                    CodeBlock.builder()
                            .add("${it.kotlinFieldName}, ")
                            .build()
                }
                is File.Field.OneOf -> TODO()
            }
        }.forEach {
            code.add(it)
        }
        code.add("unknownFields")
                .addStatement(")")
                .addStatement("return obj")

        val build = FunSpec.builder("build")
                .returns(typeName)
                .addCode(code.build())

        typeSpec.addFunction(build.build())

        return typeSpec.build()
    }

    //TODO: use these instead of re-creating defaults in Builder & Constructor
    private fun createDefaultConstants(type: File.Type.Message): List<PropertySpec> {
        return type.fields.map {
            when (it) {
                is File.Field.Standard -> {
                    val type = when {
                        it.repeated && !it.map -> {
                            List::class.asTypeName().parameterizedBy(it.kotlinValueType(false))
                        }
                        else -> it.kotlinValueType(true)
                    }
                    PropertySpec.builder("DEFAULT_${it.kotlinFieldName.capitalize().toSnakeCase().toUpperCase()}", type)
                            .initializer(it.defaultValue)
                            .jvmField()
                            .build()
                }
                is File.Field.OneOf -> TODO()
            }


        }
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
        type == File.Field.Type.MESSAGE -> "$kotlinQualifiedTypeName()"
        else -> type.defaultValue
    }

    private fun createMessageMergeExtension(type: File.Type.Message, typeName: ClassName): FunSpec {
        val codeBlock = CodeBlock.builder()
                .add("val obj = ")
                .addStatement("other?.copy(")
        type.fields.mapNotNull {
            when (it) {
                is File.Field.Standard -> buildStandardFieldMerge(it)
                is File.Field.OneOf -> buildOneOfFieldMerge(it)
            }
        }.forEach {
            codeBlock.addStatement("$it,")
        }
        codeBlock.addStatement("unknownFields = unknownFields + other.unknownFields")
                .addStatement(") ?: this")
                .addStatement("return obj")
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

    private fun createDecodeFunction(type: File.Type.Message, typeName: ClassName): FunSpec {
        return FunSpec.builder("decode")
                .returns(typeName)
                .addParameter("arr", ByteArray::class)
                .addAnnotation(JvmStatic::class)
                .addCode("return protoUnmarshal(arr)\n")
                .build()
    }

    private fun createProtoUnmarshalFunction(typeName: ClassName): FunSpec {
        val unMarshalParameter = ParameterSpec.builder("protoUnmarshal", Unmarshaller::class).build()

        return FunSpec.builder("protoUnmarshal")
                .addParameter(unMarshalParameter)
                .returns(typeName)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                        CodeBlock.builder()
                                .add("return Companion.protoUnmarshal(protoUnmarshal)")
                                .build()
                )

                .build()
    }

    private fun createCompanionProtoUnmarshalFunction(type: File.Type.Message, typeName: ClassName): FunSpec {
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
                .addStatement("0 -> return ${typeName.simpleName}(\n${doneKotlinFields.map { "$it!!" }.joinToString()}${if(doneKotlinFields.isNotEmpty()) ", " else ""}${unMarshalParameter.name}.unknownFields()\n)")
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

    private fun createEncodeFunction(): FunSpec {
        return FunSpec.builder("encode")
                .returns(ByteArray::class)
                .addCode("return protoMarshal()\n")
                .build()

    }

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
        codeBlock.addStatement("protoSize += unknownFields.entries.sumBy {\n it.value.size()\n }")

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
                codeBlock.add("%T.tagSize($number) + %T.packedRepeatedSize($kotlinFieldName, %T::${type.sizeMethod})", sizer, sizer, sizer)
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
        if (!mapEntry) null else (fields[1] as File.Field.Standard).kotlinValueType(false)

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