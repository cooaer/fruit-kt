package io.github.fruit.retrofit.converter

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class GlobalConverterFactory private constructor() : Converter.Factory() {
    private val factories = mutableMapOf<Class<out Annotation>, Converter.Factory>()

    fun add(factory: Converter.Factory, annotation: Class<out Annotation>): GlobalConverterFactory {
        factories[annotation] = factory
        return this
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        for (annotation in annotations) {
            val factory = factories[annotation.annotationClass.java]
            if (factory != null) {
                return factory.responseBodyConverter(type, annotations, retrofit)
            }
        }
        return null
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        for (annotation in methodAnnotations) {
            val factory = factories[annotation.annotationClass.java]
            if (factory != null) {
                return factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
            }
        }
        return null
    }

    companion object {
        fun create(): GlobalConverterFactory {
            return GlobalConverterFactory()
        }
    }
}
