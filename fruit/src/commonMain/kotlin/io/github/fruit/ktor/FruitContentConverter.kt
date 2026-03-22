package io.github.fruit.ktor

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.github.fruit.Fruit
import io.ktor.utils.io.core.*

class FruitContentConverter(private val fruit: Fruit) : ContentConverter {

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
    ): OutgoingContent? {
        // HTML serialization is not supported
        return null
    }
}

/**
 * Register Fruit converter in Ktor ContentNegotiation
 */
fun ContentNegotiationConfig.fruit(fruit: Fruit) {
    val converter = FruitContentConverter(fruit)
    register(ContentType.Text.Html, converter)
}
