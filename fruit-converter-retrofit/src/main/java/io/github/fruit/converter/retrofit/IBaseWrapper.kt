package io.github.fruit.converter.retrofit

/**
 * Created by ghui on 24/07/2017.
 * For compatibility with V2compose, we use setResponse.
 */
interface IBaseWrapper {
    fun setResponse(html: String)
    fun getResponse(): String
}
