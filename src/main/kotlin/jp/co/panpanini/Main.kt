package jp.co.panpanini

import pbandk.gen.pb.CodeGeneratorRequest
import pbandk.gen.pb.CodeGeneratorResponse

fun main(args: Array<String>) {
    val request = System.`in`.readBytes().let {
        CodeGeneratorRequest.protoUnmarshal(it)
    }

    val generator = CodeGenerator(request)

    val code = generator.generate()

    val response = CodeGeneratorResponse(file = code)

    System.out.write(response.protoMarshal())

}
