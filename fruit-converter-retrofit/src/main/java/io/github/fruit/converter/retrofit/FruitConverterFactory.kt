package io.github.fruit.converter.retrofit

import io.github.fruit.Fruit
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import kotlin.reflect.KClass

class FruitConverterFactory(private val fruit: Fruit) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val rawType = type as? Class<*> ?: return null
        @Suppress("UNCHECKED_CAST")
        val clazz = rawType.kotlin as KClass<Any>
        return FruitResponseBodyConverter(fruit, clazz)
    }

    companion object {
        @JvmStatic
        fun create(fruit: Fruit): FruitConverterFactory {
            return FruitConverterFactory(fruit)
        }
    }
}

class FruitResponseBodyConverter<T : Any>(
    private val fruit: Fruit,
    private val clazz: KClass<T>
) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        val html = value.string()
        return fruit.fromHtml(html, clazz)
    }
}
