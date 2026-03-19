package io.github.fruit

import io.github.fruit.annotations.Pick
import io.github.fruit.bind.BasicPickAdapters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FruitParityTest {

    @Test
    fun testOriginalFeatureParity() {
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

        val fruit = Fruit()

        // 1. 模拟 KSP 为 Author 生成的适配器
        val authorAdapter = object : PickAdapter<Author> {
            override fun read(element: com.fleeksoft.ksoup.nodes.Element, pick: Pick?): Author {
                return Author(
                    name = BasicPickAdapters.STRING_ADAPTER.read(element, Pick(".author")) ?: "",
                    date = BasicPickAdapters.STRING_ADAPTER.read(element, Pick(".date")) ?: ""
                )
            }
        }
        fruit.registerAdapter(Author::class, authorAdapter)

        // 2. 模拟 KSP 为 Article 生成的适配器 (涵盖嵌套、列表、属性、ownText)
        val articleAdapter = object : PickAdapter<Article> {
            override fun read(element: com.fleeksoft.ksoup.nodes.Element, pick: Pick?): Article {
                val container = element.selectFirst(".container")!!
                return Article(
                    title = BasicPickAdapters.STRING_ADAPTER.read(container, Pick(".title")) ?: "",
                    author = fruit.fromHtml(container.selectFirst(".meta")!!, Author::class)!!,
                    tags = container.select(".tags .tag").map { BasicPickAdapters.STRING_ADAPTER.read(it, null) ?: "" },
                    sourceUrl = BasicPickAdapters.STRING_ADAPTER.read(container, Pick(".source", attr = "href")) ?: "",
                    // 验证 ownText: 应该只得到 "This is the content."，忽略 <b> 和 <p>
                    contentSummary = BasicPickAdapters.STRING_ADAPTER.read(container, Pick(".content", ownText = true))?.trim() ?: ""
                )
            }
        }
        fruit.registerAdapter(Article::class, articleAdapter)

        // 执行解析（这是原项目最核心的用法）
        val article = fruit.fromHtml(html, Article::class)

        // 验证功能对标
        assertNotNull(article)
        assertEquals("Fruit KMP Release", article.title)
        assertEquals("Ghui", article.author.name)
        assertEquals(3, article.tags.size)
        assertEquals("Kotlin", article.tags[0])
        assertEquals("https://github.com/ghuiii/Fruit", article.sourceUrl)
        
        // 关键点验证：KMP 环境下对 HTML 结构的理解是否与 Jsoup 一致
        assertTrue(article.contentSummary.contains("This is the"))
        assertTrue(!article.contentSummary.contains("main")) // ownText 应该排除子标签
    }
}

data class Author(val name: String, val date: String)
data class Article(
    val title: String,
    val author: Author,
    val tags: List<String>,
    val sourceUrl: String,
    val contentSummary: String
)
