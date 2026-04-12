package io.github.fruit.sample.ksp

import io.github.fruit.Fruit
import io.github.fruit.registerGeneratedAdapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeneratedAdaptersTest {

    @Test
    fun generatedRegistryParsesAnnotatedType() {
        val html = """
            <article class="article">
                <h1>Generated Adapter</h1>
                <a href="https://example.com/generated">Read</a>
            </article>
        """.trimIndent()

        val fruit = Fruit().apply {
            registerGeneratedAdapters()
        }

        val result = fruit.fromHtml(html, GeneratedNews::class)

        assertNotNull(result)
        assertEquals("Generated Adapter", result?.title)
        assertEquals("https://example.com/generated", result?.link)
    }
}
