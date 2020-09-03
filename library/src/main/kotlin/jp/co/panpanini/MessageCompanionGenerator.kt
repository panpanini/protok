package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import pbandk.gen.File

class MessageCompanionGenerator(private val file: File, private val kotlinTypeMappings: Map<String, String>) {

    fun buildCompanion(type: File.Type.Message, typeName: ClassName): TypeSpec {
        val companion = TypeSpec.companionObjectBuilder()
                .addSuperinterface(Message.Companion::class.asClassName().parameterizedBy(typeName))
                .addFunction(createCompanionProtoUnmarshalFunction(type, typeName))
                .addFunction(createDecodeFunction(typeName))
                .addFunction(createWithFunction(typeName))

        createDefaultConstants(type)
                .forEach { companion.addProperty(it) }

        return companion.build()
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
                            .addStatement("var ${it.kotlinFieldName}: $typeName.${it.kotlinTypeName} = $typeName.${it.kotlinTypeName}.NotSet")
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
                .addStatement("0 ->·return·${createBuilderInitFunction(type.fields, doneKotlinFields)}")
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
                fieldBlock.addStatement("${field.kotlinFieldName} = ${field.unmarshalReadExpression(kotlinTypeMappings)}")
            } else {
                val oneOfType = "${typeName.simpleName}.${oneOf.kotlinTypeName}.${oneOf.kotlinFieldTypeNames[field.name]}"
                require(!field.repeated)
                fieldBlock.addStatement("${oneOf.kotlinFieldName} = $oneOfType(${field.unmarshalReadExpression(kotlinTypeMappings)})")
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

    private fun createBuilderInitFunction(fields: List<File.Field>, doneValue: List<String>): CodeBlock {
        return CodeBlock.builder()
                .add("Builder()\n")
                .apply {
                    fields.mapIndexed { index, field ->
                        ".${field.kotlinFieldName}(${doneValue[index]})"
                    }
                            .forEach {
                                addStatement(it)
                            }
                }
                .addStatement(".unknownFields(protoUnmarshal.unknownFields())")
                .addStatement(".build()")
                .build()
    }

    private fun createDecodeFunction(typeName: ClassName): FunSpec {
        return FunSpec.builder("decode")
                .returns(typeName)
                .addParameter("arr", ByteArray::class)
                .addAnnotation(JvmStatic::class)
                .addCode("return protoUnmarshal(arr)\n")
                .build()
    }

    /**
     * fun with(block: Builder.() -> Unit): Thing {
    return Thing().copy(block)
    }
     */

    private fun createWithFunction(typeName: ClassName): FunSpec {
        val builderParameter = ParameterSpec.builder(
                "block",
                LambdaTypeName.get(ClassName("", "Builder"), returnType = Unit::class.asTypeName())
        )

        return FunSpec.builder("with")
                .addParameter(builderParameter.build())
                .addCode(
                        CodeBlock.builder()
                                .addStatement("return %T().copy(block)", typeName)
                                .build()
                )
                .build()
    }

    private fun createDefaultConstants(type: File.Type.Message): List<PropertySpec> {
        return type.fields.map {
            when (it) {
                is File.Field.Standard -> {
                    val type = when {
                        it.repeated && !it.map -> {
                            it.kotlinValueType(false)
                        }
                        else -> it.kotlinValueType(false)
                    }
                    PropertySpec.builder(it.defaultValueName, type)
                            .initializer(it.defaultValue(file.version, kotlinTypeMappings))
                            .jvmField()
                            .build()
                }
                is File.Field.OneOf -> PropertySpec.builder(it.defaultValueName, it.type)
                        .initializer(it.defaultValue)
                        .jvmField()
                        .build()
            }
        }
    }

    private fun File.Field.Standard.unmarshalLocalVar(): CodeBlock {
        val codeBlock = CodeBlock.builder()

        when {
            repeated -> {
                mapEntry().let {
                    if (it == null) {
                        codeBlock.addStatement("var $kotlinFieldName: %T = emptyList()", List::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName(kotlinTypeMappings)).copy(nullable = false))
                    } else {
                        codeBlock.addStatement("var $kotlinFieldName: %T = emptyMap()", Map::class.asTypeName().parameterizedBy(it.mapEntryKeyKotlinType!!, it.mapEntryValueKotlinType!!).copy(nullable = false))
                    }
                }
            }
            requiresExplicitTypeWithVal -> {
                codeBlock.addStatement("var $kotlinFieldName: ${kotlinValueType(false)} = ${defaultValue(file.version, kotlinTypeMappings)}")
            }
            else -> {
                codeBlock.addStatement("var $kotlinFieldName = ${defaultValue(file.version, kotlinTypeMappings)}")
            }
        }
        return codeBlock.build()
    }

    private val File.Field.Standard.unmarshalVarDone get() =
        when {
            map -> "HashMap($kotlinFieldName)"
            repeated -> kotlinFieldName
            else -> kotlinFieldName
        }

    private fun File.Field.Standard.mapEntry() =
            if (!map) null else (localType(file) as? File.Type.Message)?.takeIf { it.mapEntry }

    private val File.Field.Standard.requiresExplicitTypeWithVal get() =
        repeated || (file.version == 2 && optional) || type.requiresExplicitTypeWithVal

    private fun File.Field.Standard.kotlinValueType(nullableIfMessage: Boolean): TypeName = when {
        map -> mapEntry()!!.let {
            val key = it.mapEntryKeyKotlinType ?: return@let kotlinQualifiedTypeName(kotlinTypeMappings)
            val value = it.mapEntryValueKotlinType ?: return@let kotlinQualifiedTypeName(kotlinTypeMappings)

            val param = Map::class.asTypeName().parameterizedBy(key, value)

            param
        }
        repeated -> List::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName(kotlinTypeMappings))
        (file.version == 2 && optional) || (type == File.Field.Type.MESSAGE && nullableIfMessage) ->
            kotlinQualifiedTypeName(kotlinTypeMappings).copy(nullable = true)
        else -> kotlinQualifiedTypeName(kotlinTypeMappings)
    }

    private val File.Type.Message.mapEntryKeyKotlinType get() =
        if (!mapEntry) null else (fields[0] as File.Field.Standard).kotlinValueType(false)
    private val File.Type.Message.mapEntryValueKotlinType get() =
        if (!mapEntry) null else (fields[1] as File.Field.Standard).kotlinValueType(false)

    private fun File.Type.Message.sortedStandardFieldsWithOneOfs() =
            fields.flatMap {
                when (it) {
                    is File.Field.Standard -> listOf(it to null)
                    is File.Field.OneOf -> it.fields.map { f -> f to it }
                }
            }.sortedBy { (field, _) ->  field.number }
}