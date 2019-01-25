package jp.co.panpanini

import pbandk.gen.pb.CodeGeneratorRequest
import pbandk.gen.pb.CodeGeneratorResponse

class CodeGenerator(private val request: CodeGeneratorRequest) {

    fun generate(): List<CodeGeneratorResponse.File> {
        val typeMappings = mutableMapOf<String, String>()

        return request.protoFile.filter {
            // only take files that need to be generated
            val name = it.name ?: return@filter false //TODO
            request.fileToGenerate.contains(name)
            true
        }.flatMap {
            val (file, types) = FileGenerator().generateFile(it, typeMappings)
            typeMappings += types
            file
        }
    }
}