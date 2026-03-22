package io.github.fruit

/**
 * For compatibility with V2compose, we use setResponse.
 */
interface IBaseWrapper {
    fun setResponse(html: String)
    fun getResponse(): String
}
