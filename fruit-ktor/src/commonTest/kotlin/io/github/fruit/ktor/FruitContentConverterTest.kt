package io.github.fruit.ktor

import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.Fruit
import io.github.fruit.PickAdapter
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FruitContentConverterTest {

    @Test
    fun deserializeHtmlIntoRegisteredType() = runTest {
        val fruit = Fruit().apply {
            registerAdapter(KtorNews::class, KtorNewsAdapter)
        }

        val converter = FruitContentConverter(fruit)
        val html = """
            <article class="story">
                <h1>Ktor Extension</h1>
                <a href="https://example.com/ktor">Read</a>
            </article>
        """.trimIndent()

        val result = converter.deserialize(
            charset = Charsets.UTF_8,
            typeInfo = typeInfo<KtorNews>(),
            content = ByteReadChannel(html.encodeToByteArray())
        ) as KtorNews?

        assertNotNull(result)
        assertEquals("Ktor Extension", result.title)
        assertEquals("https://example.com/ktor", result.link)
    }
}

private data class KtorNews(
    val title: String,
    val link: String
)

private object KtorNewsAdapter : PickAdapter<KtorNews> {
    override fun read(
        element: Element,
        css: String,
        attr: String,
        ownText: Boolean
    ): KtorNews? {
        val root = if (css.isEmpty()) element else element.selectFirst(css) ?: return null
        val title = root.selectFirst("h1")?.text().orEmpty()
        val link = root.selectFirst("a")?.attr("href").orEmpty()
        return KtorNews(title = title, link = link)
    }
}
