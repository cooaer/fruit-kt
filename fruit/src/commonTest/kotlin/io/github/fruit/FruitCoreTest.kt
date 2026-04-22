package io.github.fruit

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.annotations.Attrs
import io.github.fruit.bind.BasicSliceAdapters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class FruitCoreTest {

    @Test
    fun parseNestedModelsListsAndOwnText() {
        val html = """
            <div class="container">
                <h1 class="title">Fruit KMP Release</h1>
                <div class="meta">
                    <span class="author">Ghui</span>
                    <span class="date">2026-03-19</span>
                </div>
                <ul class="tags">
                    <li class="tag">Kotlin</li>
                    <li class="tag">Multiplatform</li>
                    <li class="tag">Mobile</li>
                </ul>
                <div class="content">
                    This is the <b>main</b> content.
                    <p>Sub-content</p>
                </div>
                <a class="source" href="https://github.com/ghuiii/Fruit">View Source</a>
            </div>
        """.trimIndent()

        val fruit = Fruit().apply {
            registerSliceAdapter(Author::class, AuthorAdapter)
            registerSliceAdapter(Article::class, ArticleAdapter(this))
        }

        val article = fruit.fromHtml(html, Article::class)

        assertNotNull(article)
        assertEquals("Fruit KMP Release", article.title)
        assertEquals("Ghui", article.author.name)
        assertEquals(3, article.tags.size)
        assertEquals("Kotlin", article.tags.first())
        assertEquals("https://github.com/ghuiii/Fruit", article.sourceUrl)
        assertEquals("This is the content.", article.contentSummary)
    }

    @Test
    fun basicSliceAdaptersReadPrimitiveValues() {
        val html = """
            <div>
                <span class="count">42</span>
                <span class="price">9.5</span>
                <span class="enabled">yes</span>
                <a class="source" href="https://example.com/item">Item</a>
            </div>
        """.trimIndent()
        val document = Ksoup.parse(html)

        assertEquals("42", BasicSliceAdapters.STRING_ADAPTER.read(document, ".count"))
        assertEquals(42, BasicSliceAdapters.INT_ADAPTER.read(document, ".count"))
        assertEquals(9.5f, BasicSliceAdapters.FLOAT_ADAPTER.read(document, ".price"))
        assertEquals(true, BasicSliceAdapters.BOOLEAN_ADAPTER.read(document, ".enabled"))
        assertEquals(
            "https://example.com/item",
            BasicSliceAdapters.STRING_ADAPTER.read(document, ".source", Attrs.HREF)
        )
    }

    @Test
    fun throwWhenAdapterIsMissing() {
        val fruit = Fruit()

        assertFailsWith<IllegalArgumentException> {
            fruit.fromHtml("<div />", UnknownArticle::class)
        }
    }
}

private data class Author(
    val name: String,
    val date: String
)

private data class Article(
    val title: String,
    val author: Author,
    val tags: List<String>,
    val sourceUrl: String,
    val contentSummary: String
)

private class ArticleAdapter(
    private val fruit: Fruit
) : SliceAdapter<Article> {
    override fun read(
        element: Element,
        css: String,
        attr: String,
        ownText: Boolean
    ): Article? {
        val root = resolveElement(element, css) ?: return null
        val contentSummary = BasicSliceAdapters.STRING_ADAPTER
            .read(root, ".content", ownText = true)
            .orEmpty()
            .replace("\\s+".toRegex(), " ")
            .trim()

        return Article(
            title = BasicSliceAdapters.STRING_ADAPTER.read(root, ".title").orEmpty(),
            author = fruit.fromHtml(root.selectFirst(".meta") ?: return null, Author::class) ?: return null,
            tags = root.select(".tags .tag").map { BasicSliceAdapters.STRING_ADAPTER.read(it).orEmpty() },
            sourceUrl = BasicSliceAdapters.STRING_ADAPTER.read(root, ".source", Attrs.HREF).orEmpty(),
            contentSummary = contentSummary
        )
    }
}

private object AuthorAdapter : SliceAdapter<Author> {
    override fun read(
        element: Element,
        css: String,
        attr: String,
        ownText: Boolean
    ): Author? {
        val root = resolveElement(element, css) ?: return null
        return Author(
            name = BasicSliceAdapters.STRING_ADAPTER.read(root, ".author").orEmpty(),
            date = BasicSliceAdapters.STRING_ADAPTER.read(root, ".date").orEmpty()
        )
    }
}

private data class UnknownArticle(
    val title: String = ""
)

private fun resolveElement(element: Element, css: String): Element? {
    return if (css.isEmpty()) element else element.selectFirst(css)
}
