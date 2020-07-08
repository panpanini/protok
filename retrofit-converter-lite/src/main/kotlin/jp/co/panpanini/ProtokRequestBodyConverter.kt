package jp.co.panpanini

import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

class ProtokRequestBodyConverter<T : Message<*>> : Converter<T, RequestBody> {

    override fun convert(value: T): RequestBody? {
        val body = RequestBody.create(MediaType.get("application/x-protobuf"), value.protoMarshal())
        print(body.contentLength())
        return body
    }
}