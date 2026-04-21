package io.github.fruit.sample.ksp

import io.github.fruit.Fruit
import io.github.fruit.registerGeneratedAdapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedAdaptersTest {

    @Test
    fun generatedRegistryParsesAnnotatedType() {
        val html = """
            <article class="article">
                <h1>Generated Adapter</h1>
                <a href="https://example.com/generated">Read</a>
                <div class="meta"><span class="author">Codex</span></div>
                <div class="tags">
                    <span class="tag">ksp</span>
                    <span class="tag">data-class</span>
                </div>
            </article>
        """.trimIndent()

        val fruit = Fruit().apply {
            registerGeneratedAdapters()
        }

        val result = fruit.fromHtml(html, GeneratedNews::class)

        assertNotNull(result)
        assertEquals("Generated Adapter", result?.title)
        assertEquals("https://example.com/generated", result?.link)
        assertEquals(listOf("ksp", "data-class"), result?.tags?.map { it.name })
        assertEquals("Codex", result?.meta?.author)
        assertTrue(result?.rawResponse?.contains("<article class=\"article\">") == true)
    }
}
