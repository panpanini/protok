package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import pbandk.Message
import pbandk.gen.File

class EnumGenerator {

    fun buildEnum(type: File.Type.Enum): TypeSpec {
        val className = ClassName("", type.kotlinTypeName)
        val typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("value", Int::class, KModifier.OVERRIDE)
                                .build()
                )
                .addSuperinterface(Message.Enum::class)

        val companion = TypeSpec.companionObjectBuilder()
                .addSuperinterface(Message.Enum.Companion::class.asClassName().parameterizedBy(className))


        type.values.forEach {
            companion.addProperty(
                    PropertySpec.builder(
                            it.kotlinValueName,
                            ClassName.bestGuess(type.kotlinTypeName)
                    )
                            .initializer("%T(${it.number})", ClassName.bestGuess(type.kotlinTypeName))
                            .addAnnotation(JvmField::class)
                            .build()
            )
        }
        companion.addFunction(createFromValueFunction(type.values, className))
        typeSpec.addProperty(PropertySpec.builder("value", Int::class).initializer("value").build())
        typeSpec.addType(companion.build())
        return typeSpec.build()
    }

    private fun createFromValueFunction(values: List<File.Type.Enum.Value>, type: ClassName): FunSpec {
        val whenBlock = CodeBlock.builder()
                .beginControlFlow("return when(value)")
        values.forEach {
            whenBlock.addStatement("${it.number} -> ${it.kotlinValueName}")
        }
        whenBlock.addStatement("else -> %T(value)", type)
        whenBlock.endControlFlow()

        return FunSpec.builder("fromValue")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("value", Int::class)
                .returns(type)
                .addCode(whenBlock.build())
                .build()
    }
}