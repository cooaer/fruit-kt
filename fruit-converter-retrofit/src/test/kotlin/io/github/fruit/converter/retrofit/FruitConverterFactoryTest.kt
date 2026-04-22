package io.github.fruit.converter.retrofit

import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.Fruit
import io.github.fruit.SliceAdapter
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit

class FruitConverterFactoryTest {

    @Test
    fun convertHtmlResponseBody() {
        val fruit = Fruit().apply {
            registerSliceAdapter(RetrofitNews::class, RetrofitNewsAdapter)
        }

        val factory = FruitConverterFactory.create(fruit)
        @Suppress("UNCHECKED_CAST")
        val converter = factory.responseBodyConverter(
            RetrofitNews::class.java,
            emptyArray(),
            Retrofit.Builder().baseUrl("https://example.com/").build()
        ) as retrofit2.Converter<okhttp3.ResponseBody, RetrofitNews>

        val html = """
            <article>
                <h1>Retrofit Extension</h1>
                <a href="https://example.com/retrofit">Read</a>
            </article>
        """.trimIndent()

        val result = converter.convert(
            ResponseBody.create(MediaType.parse("text/html"), html)
        )

        assertNotNull(result)
        assertEquals("Retrofit Extension", result?.title)
        assertEquals("https://example.com/retrofit", result?.link)
    }
}

private data class RetrofitNews(
    val title: String,
    val link: String
)

private object RetrofitNewsAdapter : SliceAdapter<RetrofitNews> {
    override fun read(
        element: Element,
        css: String,
        attr: String,
        ownText: Boolean
    ): RetrofitNews? {
        val root = if (css.isEmpty()) element else element.selectFirst(css) ?: element
        return RetrofitNews(
            title = root.selectFirst("h1")?.text().orEmpty(),
            link = root.selectFirst("a")?.attr("href").orEmpty()
        )
    }
}
