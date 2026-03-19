package io.github.fruit.bind

import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.PickAdapter
import io.github.fruit.annotations.Attrs

object BasicPickAdapters {
    
    private fun readRaw(element: Element, css: String, attr: String, ownText: Boolean): String? {
        var el = element
        if (css.isNotEmpty()) {
            el = el.selectFirst(css) ?: return null
        }
        
        return when (attr) {
            Attrs.TEXT -> if (ownText) el.ownText() else el.text()
            Attrs.HTML -> el.html()
            Attrs.OUTER_HTML -> el.outerHtml()
            else -> el.attr(attr)
        }
    }

    val STRING_ADAPTER = object : PickAdapter<String> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): String = 
            readRaw(element, css, attr, ownText) ?: ""
    }

    val INT_ADAPTER = object : PickAdapter<Int> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Int = 
            readRaw(element, css, attr, ownText)?.toIntOrNull() ?: 0
    }

    val LONG_ADAPTER = object : PickAdapter<Long> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Long = 
            readRaw(element, css, attr, ownText)?.toLongOrNull() ?: 0L
    }

    val FLOAT_ADAPTER = object : PickAdapter<Float> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Float = 
            readRaw(element, css, attr, ownText)?.toFloatOrNull() ?: 0.0f
    }

    val BOOLEAN_ADAPTER = object : PickAdapter<Boolean> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Boolean {
            val raw = readRaw(element, css, attr, ownText) ?: return false
            return raw.lowercase().let { it == "true" || it == "1" || it == "yes" }
        }
    }
}
