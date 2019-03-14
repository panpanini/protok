package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import pbandk.ListWithSize
import pbandk.MessageMap
import pbandk.Unmarshaller
import pbandk.gen.File

class MessageCompanionGenerator(private val file: File, private val kotlinTypeMappings: Map<String, String>) {

    fun buildCompanion(type: File.Type.Message, typeName: ClassName): TypeSpec {
        val companion = TypeSpec.companionObjectBuilder()
                .addSuperinterface(Message.Companion::class.asClassName().parameterizedBy(typeName))
                .addFunction(createCompanionProtoUnmarshalFunction(type, typeName))
                .addFunction(createDecodeFunction(typeName))

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
                .addStatement("0 ->·return·${typeName.simpleName}(${doneKotlinFields.map { "$it" }.joinToString()}${if(doneKotlinFields.isNotEmpty()) ",·" else ""}${unMarshalParameter.name}.unknownFields())")
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

    private fun createDecodeFunction(typeName: ClassName): FunSpec {
        return FunSpec.builder("decode")
                .returns(typeName)
                .addParameter("arr", ByteArray::class)
                .addAnnotation(JvmStatic::class)
                .addCode("return protoUnmarshal(arr)\n")
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
                is File.Field.OneOf -> TODO()
            }
        }
    }

    private fun File.Field.Standard.unmarshalLocalVar(): CodeBlock {
        val codeBlock = CodeBlock.builder()

        when {
            repeated -> {
                mapEntry().let {
                    if (it == null) {
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", ListWithSize.Builder::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName(kotlinTypeMappings)).copy(nullable = true))
                    } else {
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", MessageMap.Builder::class.asTypeName().parameterizedBy(it.mapEntryKeyKotlinType!!, it.mapEntryValueKotlinType!!).copy(nullable = true))
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
            map -> "pbandk.MessageMap.Builder.fixed($kotlinFieldName)"
            repeated -> "pbandk.ListWithSize.Builder.fixed($kotlinFieldName).list"
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