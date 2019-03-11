package jp.co.panpanini

import com.improve_future.case_changer.toSnakeCase
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
                            .initializer(it.defaultValue)
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
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", ListWithSize.Builder::class.asTypeName().parameterizedBy(kotlinQualifiedTypeName).copy(nullable = true))
                    } else {
                        codeBlock.addStatement("var $kotlinFieldName: %T = null", MessageMap.Builder::class.asTypeName().parameterizedBy(it.mapEntryKeyKotlinType!!, it.mapEntryValueKotlinType!!).copy(nullable = true))
                    }
                }
            }
            requiresExplicitTypeWithVal -> {
                codeBlock.addStatement("var $kotlinFieldName: ${kotlinValueType(false)} = $defaultValue")
            }
            else -> {
                codeBlock.addStatement("var $kotlinFieldName = $defaultValue")
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
            if (!map) null else (localType as? File.Type.Message)?.takeIf { it.mapEntry }

    private val File.Field.Standard.requiresExplicitTypeWithVal get() =
        repeated || (file.version == 2 && optional) || type.requiresExplicitTypeWithVal

    private val File.Field.Standard.localType get() = localTypeName?.let { findLocalType(it) }

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

    private val File.Type.Message.mapEntryKeyKotlinType get() =
        if (!mapEntry) null else (fields[0] as File.Field.Standard).kotlinValueType(false)
    private val File.Type.Message.mapEntryValueKotlinType get() =
        if (!mapEntry) null else (fields[1] as File.Field.Standard).kotlinValueType(false)


    private val File.Field.Standard.defaultValue get() = when {
        map -> "emptyMap()"
        repeated -> "emptyList()"
        file.version == 2 && optional -> "null"
        type == File.Field.Type.ENUM -> "$kotlinQualifiedTypeName.fromValue(0)"
        type == File.Field.Type.MESSAGE -> "$kotlinQualifiedTypeName()"
        else -> type.defaultValue
    }

    private val File.Field.Standard.kotlinQualifiedTypeName: TypeName
        get() {
            return when {
                kotlinLocalTypeName != null -> ClassName("", kotlinLocalTypeName!!)
                localTypeName != null -> ClassName("", kotlinTypeMappings.getOrElse(localTypeName!!) { error("Unable to find mapping for $localTypeName") })
                else -> type.standardTypeName
            }
        }

    private val File.Field.defaultValueName : String
        get() = "DEFAULT_${this.kotlinFieldName.capitalize().toSnakeCase().toUpperCase()}"

    private fun File.Type.Message.sortedStandardFieldsWithOneOfs() =
            fields.flatMap {
                when (it) {
                    is File.Field.Standard -> listOf(it to null)
                    is File.Field.OneOf -> it.fields.map { f -> f to it }
                }
            }.sortedBy { (field, _) ->  field.number }

    private val File.Field.Standard.tag get() = (number shl 3) or when {
        repeated && packed -> 2
        else -> type.wireFormat
    }

    private val File.Field.Standard.unmarshalReadExpression get() = type.neverPacked.let { neverPacked ->
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


}