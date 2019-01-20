package jp.co.panpanini

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import pbandk.gen.File
import pbandk.gen.FileBuilder
import pbandk.gen.pb.CodeGeneratorResponse
import pbandk.wkt.FileDescriptorProto

class FileGenerator {

    fun generateFile(it: FileDescriptorProto, typeMappings: Map<String, String>): Pair<CodeGeneratorResponse.File?,  Map<String, String>>{
        val file = FileBuilder.buildFile(FileBuilder.Context(it, mapOf()))
        val types = typeMappings + file.kotlinTypeMappings()

        val packageName = file.kotlinPackageName ?: return Pair(null, typeMappings)
        val name = it.name ?: return Pair(null, typeMappings)
        val fileSpec = FileSpec.builder(packageName, name)
        file.types.map { type ->
            type.toTypeSpec(file, types)
        }.forEach { typeSpec ->
            fileSpec.addType(typeSpec)
        }
        fileSpec.build()

        val fileNameSansPath = name.substringAfterLast('/')
        val filePath = "${packageName.replace('.', '/')}/${fileNameSansPath.removeSuffix(".proto")}.kt"

        return Pair(CodeGeneratorResponse.File(name = filePath, content = fileSpec.build().toString()), types)
    }
}


fun File.Type.toTypeSpec(file: File, types: Map<String, String>): TypeSpec = when(this) {
    is File.Type.Enum -> EnumGenerator().buildEnum(this)
    is File.Type.Message -> MessageGenerator(file, types).buildMessage(this)
}