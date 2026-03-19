package io.github.fruit

import com.fleeksoft.ksoup.Ksoup
import io.github.fruit.annotations.Pick
import io.github.fruit.bind.BasicPickAdapters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FruitTest {

    @Test
    fun testComprehensiveParsing() {
        val html = """
            <div id="author">
                <span class="name">Ghui</span>
                <span class="age">28</span>
            </div>
            <ul id="tags">
                <li>Kotlin</li>
                <li>Multiplatform</li>
                <li>Fruit</li>
            </ul>
        """.trimIndent()

        val fruit = Fruit()

        // 模拟 KSP 生成的 AuthorPickAdapter
        val authorAdapter = object : PickAdapter<Author> {
            override fun read(element: com.fleeksoft.ksoup.nodes.Element, pick: Pick?): Author? {
                val subEl = element.selectFirst("#author") ?: return null
                return Author(
                    name = BasicPickAdapters.STRING_ADAPTER.read(subEl, Pick(".name")) ?: "",
                    age = BasicPickAdapters.INT_ADAPTER.read(subEl, Pick(".age")) ?: 0
                )
            }
        }
        fruit.registerAdapter(Author::class, authorAdapter)

        // 模拟 KSP 生成的 ArticlePickAdapter
        val articleAdapter = object : PickAdapter<Article> {
            override fun read(element: com.fleeksoft.ksoup.nodes.Element, pick: Pick?): Article {
                return Article(
                    author = fruit.fromHtml(element, Author::class)!!,
                    tags = element.select("#tags li").map { BasicPickAdapters.STRING_ADAPTER.read(it, null) ?: "" }
                )
            }
        }
        fruit.registerAdapter(Article::class, articleAdapter)

        // 执行解析
        val article = fruit.fromHtml(html, Article::class)
        
        assertNotNull(article)
        assertEquals("Ghui", article.author.name)
        assertEquals(28, article.author.age)
        assertEquals(3, article.tags.size)
        assertEquals("Kotlin", article.tags[0])
        assertEquals("Fruit", article.tags[2])
    }
}

data class Author(
    val name: String,
    val age: Int
)

data class Article(
    val author: Author,
    val tags: List<String>
)
