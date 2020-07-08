package jp.co.panpanini

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


class ProtokConverterFactory : Converter.Factory() {

    private val adapter = ProtokAdapter()

    override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (Message::class.java.isAssignableFrom(type as Class<*>)) {
            ProtokResponseBodyConverter(
                    adapter.getResponseAdapter(type.getDeclaredConstructor().newInstance() as Message<*>)
            )
        } else {
            null
        }
    }

    override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return if (Message::class.java.isAssignableFrom(type as Class<*>)) {
            adapter.getRequestConverter(type.getDeclaredConstructor().newInstance() as Message<*>)
        } else {
            null
        }
    }

    companion object {
        @JvmStatic
        fun create(): ProtokConverterFactory {
            return ProtokConverterFactory()
        }
    }
}

class ProtokAdapter {
    fun <T : Message<*>> getResponseAdapter(type: T): T {
        return type
    }

    fun <T : Message<*>> getRequestConverter(type: T): ProtokRequestBodyConverter<T> {
        return ProtokRequestBodyConverter()
    }
}
