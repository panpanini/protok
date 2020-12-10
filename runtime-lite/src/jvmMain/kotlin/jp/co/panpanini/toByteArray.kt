package jp.co.panpanini

actual fun String.toByteArray(): ByteArray = this.toByteArray(Charsets.UTF_8)