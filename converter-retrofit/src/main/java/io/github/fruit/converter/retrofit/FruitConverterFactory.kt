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
    ): Converter<ResponseBody, *> {
        return FruitResponseBodyConverter(fruit, type)
    }

    companion object {
        fun create(fruit: Fruit): FruitConverterFactory {
            return FruitConverterFactory(fruit)
        }
    }
}

class FruitResponseBodyConverter<T>(
    private val fruit: Fruit,
    private val type: Type
) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        val html = value.string()
        @Suppress("UNCHECKED_CAST")
        val clazz = (type as? Class<*>)?.kotlin as? KClass<Any>
            ?: throw IllegalArgumentException("Fruit converter only supports direct class binding currently.")
        
        return fruit.fromHtml(html, clazz) as? T
    }
}
