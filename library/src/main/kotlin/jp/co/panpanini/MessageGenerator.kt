package jp.co.panpanini

import com.improve_future.case_changer.beginWithUpperCase
import com.improve_future.case_changer.toSnakeCase
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import pbandk.gen.File
import java.io.Serializable

class MessageGenerator(private val file: File, private val kotlinTypeMappings: Map<String, String>) {

    private val companionGenerator = MessageCompanionGenerator(file, kotlinTypeMappings)

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

        type.fields.map { field ->
            val param = when (field) {
                is File.Field.Standard -> PropertySpec.builder(field.kotlinFieldName, field.kotlinValueType(false))
                is File.Field.OneOf -> PropertySpec.builder(field.kotlinFieldName, field.type)
            }

            param.initializer(field.defaultValue)
                    .setter(FunSpec.setterBuilder().addModifiers(KModifier.PRIVATE).build())
                    .mutable(true)

            if (type.mapEntry) {
                // add override map entry
                param.addModifiers(KModifier.OVERRIDE)
            }
            Pair(param.build(), field)
        }.forEach { (property, field) ->
            val param = ParameterSpec.builder(property.name, property.type)
            if (!type.mapEntry) {
                param.defaultValue(field.defaultValue)
            }
            typeSpec.addProperty(property)
        }
        // unknown fields
       val unknownPropertySpec = unknownFieldSpec()
        typeSpec.addProperty(unknownPropertySpec)

        typeSpec.primaryConstructor(FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE).build())

        mapEntry?.let {
            typeSpec.addSuperinterface(mapEntry)
        }

        type.nestedTypes.map {
            it.toTypeSpec(file, kotlinTypeMappings)
        }.forEach {
            typeSpec.addType(it)
        }

        // add oneof types as a subtype
        type.fields
                .mapNotNull { it as? File.Field.OneOf }
                .map(this::createOneOfType)
                .forEach { typeSpec.addType(it) }

        typeSpec.addFunction(createMessageSizeExtension(type, className))
        typeSpec.addFunction(createMessageMarshalExtension(type, className))
        typeSpec.addFunction(createMessageMergeExtension(type, className))
        typeSpec.addProperty(createProtoSizeVal())
        typeSpec.addFunction(createProtoMarshalFunction())
        typeSpec.addFunction(createPlusOperator(className))
        typeSpec.addFunction(createCopyFunction(className))
        typeSpec.addType(companionGenerator.buildCompanion(type, className))
        typeSpec.addFunction(createEncodeFunction())
        typeSpec.addFunction(createProtoUnmarshalFunction(className))
        if (!type.mapEntry) {
            typeSpec.addFunction(createNewBuilder(type, className))
            typeSpec.addType(createBuilder(type, className))
        }
        return typeSpec.build()
    }

    private fun createProtoSizeVal(): PropertySpec {
        return PropertySpec.builder("protoSize", Int::class)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                        FunSpec.getterBuilder().addCode(
                                CodeBlock.builder()
                                        .addStatement("return protoSizeImpl()")
                                        .build()
                        )
                                .build()
                )
                .build()
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

    private fun createNewBuilder(type: File.Type.Message, typeName: ClassName): FunSpec {
        val builder = ClassName("", "Builder")
        val funSpec = FunSpec.builder("newBuilder")
                .returns(builder)
        val codeBlock = CodeBlock.Builder()
                .add("return Builder()\n")

        type.fields.map {
            when (it) {

                is File.Field.Standard -> {
                    ".${it.kotlinFieldName}(${it.kotlinFieldName})"
                }
                is File.Field.OneOf -> ".${it.kotlinFieldName}(${it.kotlinFieldName})"
            }
        }.forEach {
            codeBlock.indent()
                    .add("$it\n")
                    .unindent()
        }
        //unknownFields
        codeBlock.indent()
                .add(".unknownFields(unknownFields)")
                .unindent()
                .add("\n")

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
                        .initializer(it.defaultValueName)
                        .build()
                is File.Field.OneOf -> PropertySpec.builder(it.kotlinFieldName, it.type)
                        .mutable()
                        .initializer(it.defaultValueName)
                        .build()
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
                                .addStatement("this.${it.kotlinFieldName} = ${it.kotlinFieldName} ?: ${it.defaultValueName}")
                                .addStatement("return this")
                                .build()
                        )
                        .build()
                is File.Field.OneOf -> FunSpec.builder(it.kotlinFieldName)
                        .addParameter(ParameterSpec.builder(it.kotlinFieldName, it.type.copy(nullable = true)).build())
                        .returns(builder)
                        .addCode(CodeBlock.builder()
                                .addStatement("this.${it.kotlinFieldName} = ${it.kotlinFieldName} ?: ${it.defaultValueName}")
                                .addStatement("return this")
                                .build()
                        )
                        .build()
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
                .add("return %T().apply {\n", typeName)

        type.fields.map {
            when (it) {
                is File.Field.Standard -> {
                    CodeBlock.builder()
                            .addStatement("${it.kotlinFieldName} = this@Builder.${it.kotlinFieldName}")
                            .build()
                }
                is File.Field.OneOf ->
                    CodeBlock.builder()
                            .addStatement("this@Builder.${it.kotlinFieldName}?.let { ${it.kotlinFieldName} = it }")
                            .build()
            }
        }.forEach {
            code.add(it)
        }
        code.add("unknownFields = this@Builder.unknownFields")
                .add("\n}\n")

        val build = FunSpec.builder("build")
                .returns(typeName)
                .addCode(code.build())

        typeSpec.addFunction(build.build())

        return typeSpec.build()
    }

    private fun unknownFieldSpec(): PropertySpec {
        return PropertySpec.builder(
                "unknownFields" ,
                Map::class.parameterizedBy(Int::class, UnknownField::class)
        )
                .initializer("emptyMap()")
                .mutable(true)
                .setter(FunSpec.setterBuilder().addModifiers(KModifier.PRIVATE).build())
                .build()
    }

    private val File.Field.defaultValue get() = when(this) {
        is File.Field.Standard -> defaultValue
        is File.Field.OneOf -> defaultValue

    }

    private val File.Field.Standard.defaultValue get() = when {
        map -> "emptyMap()"
        repeated -> "emptyList()"
        file.version == 2 && optional -> "null"
        type == File.Field.Type.ENUM -> "$kotlinQualifiedTypeName.fromValue(0)"
        type == File.Field.Type.MESSAGE -> "$kotlinQualifiedTypeName()"
        else -> type.defaultValue
    }

    private val File.Field.OneOf.defaultValue get() = "$kotlinTypeName.NotSet"

    private fun createCopyFunction(typeName: ClassName): FunSpec {
        val builderParameter = ParameterSpec.builder("block", LambdaTypeName.get(ClassName("", "Builder"), returnType =  Unit::class.asTypeName()))

        val code = CodeBlock.Builder()
                .add("return newBuilder()")
                .beginControlFlow(".apply")
                .addStatement("block(this)")
                .endControlFlow()
                .add(".build()\n")
                .build()

        return FunSpec.builder("copy")
                .returns(typeName)
                .addParameter(builderParameter.build())
                .addCode(code)
                .build()
    }

    private fun createMessageMergeExtension(type: File.Type.Message, typeName: ClassName): FunSpec {
        val codeBlock = CodeBlock.builder()
                .add("return ")
                .addStatement("other?.copy {")
                .indent()
        type.fields.mapNotNull {
            when (it) {
                is File.Field.Standard -> buildStandardFieldMerge(it)
                is File.Field.OneOf -> buildOneOfFieldMerge(it)
            }
        }.forEach {
            codeBlock.addStatement("$it")
        }
        codeBlock.addStatement("unknownFields = unknownFields + other.unknownFields")
                .unindent()
                .add("} ?: this\n")
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
        val codeBlock = CodeBlock.builder()
                .add("${field.kotlinFieldName}·=·")

        if (field.fields.any { it.type == File.Field.Type.MESSAGE }) {
            codeBlock.beginControlFlow("when")
            field.fields
                    .filter { it.type == File.Field.Type.MESSAGE }
                    .forEach {
                        codeBlock.beginControlFlow("this.${field.kotlinFieldName} is ${field.kotlinTypeName}.${it.kotlinFieldName.beginWithUpperCase()} && other.${field.kotlinFieldName} is ${field.kotlinTypeName}.${it.kotlinFieldName.beginWithUpperCase()} ->")
                                .addStatement("${field.kotlinTypeName}.${it.kotlinFieldName.beginWithUpperCase()}(this.${field.kotlinFieldName}.${it.kotlinFieldName} + other.${field.kotlinFieldName}.${it.kotlinFieldName})")
                                .endControlFlow()
                    }
            codeBlock.beginControlFlow("else ->")
                    .add("if·(this.${field.kotlinFieldName}·is·${field.kotlinTypeName}.NotSet)·other.${field.kotlinFieldName}·else·this.${field.kotlinFieldName}\n")
                    .endControlFlow()

            codeBlock.endControlFlow()
        } else {
            codeBlock.add("if·(this.${field.kotlinFieldName}·is·${field.kotlinTypeName}.NotSet)·other.${field.kotlinFieldName}·else·this.${field.kotlinFieldName}")
        }

        return codeBlock.build()
    }

    private fun createProtoUnmarshalFunction(typeName: ClassName): FunSpec {
        val unMarshalParameter = ParameterSpec.builder("protoUnmarshal", Unmarshaller::class).build()

        return FunSpec.builder("protoUnmarshal")
                .addParameter(unMarshalParameter)
                .returns(typeName)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                        CodeBlock.builder()
                                .add("return Companion.protoUnmarshal(protoUnmarshal)\n")
                                .build()
                )

                .build()
    }

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
                                .beginControlFlow("if (${oneOf.kotlinFieldName} is $subclassName)")
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
            val fieldBlock = when(it) {
                is File.Field.Standard -> {
                    CodeBlock.builder()
                            .beginControlFlow("if (${it.getNonDefaultCheck()})")
                            .add("protoSize += ")
                            .addStatement(it.sizeExpression().toString())
                            .endControlFlow()
                }
                is File.Field.OneOf -> {
                    val block = CodeBlock.builder()
                            .beginControlFlow("if (${it.kotlinFieldName} !is ${it.kotlinTypeName}.NotSet)")
                            .beginControlFlow("protoSize += ${it.kotlinFieldName}.run")
                            .beginControlFlow("when (this)")

                    it.fields.forEach { field ->
                        block.addStatement("is ${it.kotlinTypeName}.${field.kotlinFieldName.beginWithUpperCase()} -> ${field.sizeExpression()}")
                    }

                    block.addStatement("else -> 0")
                            .endControlFlow()
                            .endControlFlow()
                            .endControlFlow()

                    block
                }
            }

            fieldBlock.build()
        }.forEach {
            codeBlock.add(it)
        }
        // unknownFields
        codeBlock.addStatement("protoSize += unknownFields.entries.sumBy·{·it.value.size()·}")

        codeBlock.addStatement("return protoSize")

        funSpec.addCode(codeBlock.build())

        return funSpec.build()
    }

    private fun createOneOfType(field: File.Field.OneOf) : TypeSpec {
        val typeSpec = TypeSpec.classBuilder(field.type)
                .addModifiers(KModifier.SEALED)

        field.fields.map { nestedField ->
                    TypeSpec.classBuilder(nestedField.kotlinFieldName.beginWithUpperCase())
                            .superclass(field.type)
                            .addModifiers(KModifier.DATA)
                            .addProperty(
                                    PropertySpec.builder(nestedField.kotlinFieldName, nestedField.kotlinValueType(false))
                                            .initializer(nestedField.kotlinFieldName)
                                            .build()
                            )
                            .primaryConstructor(
                                    FunSpec.constructorBuilder()
                                            .addParameter(ParameterSpec.builder(nestedField.kotlinFieldName, nestedField.kotlinValueType(false))
                                                    .defaultValue(CodeBlock.of(nestedField.defaultValue))
                                                    .build())
                                            .build()
                            )
                            .build()
                }
                .forEach {
                    typeSpec.addType(it)
                }

        // default NotSet type
        typeSpec.addType(
                TypeSpec.objectBuilder("NotSet")
                        .superclass(field.type)
                        .build()
        )

        return typeSpec.build()
    }

    private fun File.Field.Standard.sizeExpression(): CodeBlock {
        val sizer = Sizer::class
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

    private fun File.Field.Standard.mapConstructorReference(): CodeBlock {
        return CodeBlock.of(
                kotlinQualifiedTypeName.let {
                    val type = it.toString()
                    type.lastIndexOf('.').let { if (it == -1) "::$type" else type.substring(0, it) + "::" + type.substring(it + 1) }
                }
        )
    }

    private fun File.Field.Standard.getNonDefaultCheck(): CodeBlock {
        return when {
            repeated -> CodeBlock.of("$kotlinFieldName.isNotEmpty()")
            file.version == 2 && optional -> CodeBlock.of("$kotlinFieldName != $defaultValueName")
            else -> CodeBlock.of("$kotlinFieldName != $defaultValueName")
        }
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

    private val File.Field.Standard.tag get() = (number shl 3) or when {
        repeated && packed -> 2
        else -> type.wireFormat
    }

    private val File.Field.defaultValueName : String
        get() = "DEFAULT_${this.kotlinFieldName.capitalize().toSnakeCase().toUpperCase()}"
}