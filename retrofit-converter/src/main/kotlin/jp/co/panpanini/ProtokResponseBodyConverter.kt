package jp.co.panpanini

import com.google.protobuf.CodedInputStream
import okhttp3.ResponseBody
import pbandk.Unmarshaller
import retrofit2.Converter

class ProtokResponseBodyConverter<T : Message<*>>(private val adapter: T) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        return adapter.protoUnmarshal(Unmarshaller(CodedInputStream.newInstance(value.byteStream()))) as? T
    }
}