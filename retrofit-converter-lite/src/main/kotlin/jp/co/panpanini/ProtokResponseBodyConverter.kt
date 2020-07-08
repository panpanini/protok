package jp.co.panpanini

import okhttp3.ResponseBody
import retrofit2.Converter

class ProtokResponseBodyConverter<T : Message<*>>(private val adapter: T) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        return value.byteStream().use(adapter::protoUnmarshal) as? T
    }
}