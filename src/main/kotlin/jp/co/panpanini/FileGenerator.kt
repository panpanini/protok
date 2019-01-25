package jp.co.panpanini

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import pbandk.gen.File
import pbandk.gen.FileBuilder
import pbandk.gen.pb.CodeGeneratorResponse
import pbandk.wkt.FileDescriptorProto

class FileGenerator {

    fun generateFile(it: FileDescriptorProto, typeMappings: Map<String, String>): Pair<List<CodeGeneratorResponse.File>,  Map<String, String>>{
        val file = FileBuilder.buildFile(FileBuilder.Context(it, mapOf()))

        val types = typeMappings + file.kotlinTypeMappings()

        val packageName = file.kotlinPackageName ?: return Pair(emptyList(), typeMappings)
        val files = file.types.map { type ->
            FileSpec.builder(packageName, type.kotlinTypeName)
                    .addType(type.toTypeSpec(file, types))
                    .build()
        }.map {
            val filePath = "${packageName.replace('.', '/')}/${it.name}.kt"
            CodeGeneratorResponse.File(name = filePath, content = it.toString())
        }

        return Pair(files, types)
    }
}


fun File.Type.toTypeSpec(file: File, types: Map<String, String>): TypeSpec = when(this) {
    is File.Type.Enum -> EnumGenerator().buildEnum(this)
    is File.Type.Message -> MessageGenerator(file, types).buildMessage(this)
}