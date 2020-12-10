package jp.co.panpanini

import java.io.InputStream

fun <T: Message<T>> Message<T>.protoUnmarshal(inputStream: InputStream) = protoUnmarshal(
   Unmarshaller(Reader(inputStream.readBytes()))
)