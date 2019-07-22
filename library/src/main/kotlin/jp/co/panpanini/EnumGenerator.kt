package jp.co.panpanini

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmStatic
import pbandk.gen.File
import java.io.Serializable

class EnumGenerator {

    fun buildEnum(type: File.Type.Enum): TypeSpec {
        val className = ClassName("", type.kotlinTypeName)
        val typeSpec = TypeSpec.enumBuilder(className)
                .addSuperinterface(Serializable::class)
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("value", Int::class, KModifier.OVERRIDE)
                                .build()
                )
                .addSuperinterface(Message.Enum::class)

        type.values.forEach {
            typeSpec.addEnumConstant(it.kotlinValueName, TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("${it.number}")
                    .build())
        }

        val companion = TypeSpec.companionObjectBuilder()
                .addSuperinterface(Message.Enum.Companion::class.asClassName().parameterizedBy(className))


        companion.addFunction(createFromValueFunction(type.values, className))
        companion.addFunction(createFromNameFunction(type.values, className))
        typeSpec.addProperty(PropertySpec.builder("value", Int::class).initializer("value").build())
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
        whenBlock.addStatement("else -> ${values.first().kotlinValueName}")
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
            whenBlock.addStatement("%S -> ${it.kotlinValueName}", it.kotlinValueName)
        }
        whenBlock.addStatement("else -> ${values.first().kotlinValueName}")
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