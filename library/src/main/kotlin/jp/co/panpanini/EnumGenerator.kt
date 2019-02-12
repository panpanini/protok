package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmStatic
import pbandk.Message
import pbandk.gen.File
import java.io.Serializable

class EnumGenerator {

    fun buildEnum(type: File.Type.Enum): TypeSpec {
        val className = ClassName("", type.kotlinTypeName)
        val typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)
                .addSuperinterface(Serializable::class)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("value", Int::class, KModifier.OVERRIDE)
                                .addParameter(ParameterSpec.builder("name", String::class).addAnnotation(JvmField::class).build())
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
                            .initializer("%T(${it.number}, %S)", ClassName.bestGuess(type.kotlinTypeName), it.name)
                            .addAnnotation(JvmField::class)
                            .build()
            )
        }
        companion.addFunction(createFromValueFunction(type.values, className))
        companion.addFunction(createFromNameFunction(type.values, className))
        typeSpec.addProperty(PropertySpec.builder("value", Int::class).initializer("value").build())
        typeSpec.addProperty(PropertySpec.builder("name", String::class).initializer("name").build())
        typeSpec.addType(companion.build())
        typeSpec.addFunction(createToStringFunction())
        return typeSpec.build()
    }

    private fun createFromValueFunction(values: List<File.Type.Enum.Value>, type: ClassName): FunSpec {
        val whenBlock = CodeBlock.builder()
                .beginControlFlow("return when(value)")
        values.forEach {
            whenBlock.addStatement("${it.number} -> ${it.kotlinValueName}")
        }
        whenBlock.addStatement("else -> %T(value, %S)", type, "")
        whenBlock.endControlFlow()

        return FunSpec.builder("fromValue")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("value", Int::class)
                .returns(type)
                .addCode(whenBlock.build())
                .jvmStatic()
                .build()
    }

    private fun createFromNameFunction(values: List<File.Type.Enum.Value>, type: ClassName): FunSpec {
        val whenBlock = CodeBlock.builder()
                .beginControlFlow("return when(name)")
        values.forEach {
            whenBlock.addStatement("%S -> ${it.kotlinValueName}", it.name)
        }
        whenBlock.addStatement("else -> %T(-1, name)", type)
        whenBlock.endControlFlow()

        return FunSpec.builder("fromName")
                .addParameter("name", String::class)
                .returns(type)
                .addCode(whenBlock.build())
                .jvmStatic()
                .build()
    }

    private fun createToStringFunction(): FunSpec {
        return FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(String::class)
                .addCode("return name")
                .build()
    }
}