package io.github.fruit.ktor

import io.github.fruit.Fruit
import io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining

class FruitContentConverter(
    private val fruit: Fruit
) : ContentConverter {

    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any? {
        val html = content.readRemaining().readText(charset = charset)
        val clazz = typeInfo.type
        return fruit.fromHtml(html, clazz)
    }

    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent? = null
}

fun ContentNegotiationConfig.fruit(fruit: Fruit) {
    register(ContentType.Text.Html, FruitContentConverter(fruit))
}
