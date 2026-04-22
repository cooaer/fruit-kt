package io.github.fruit.bind

import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.SliceAdapter
import io.github.fruit.annotations.Attrs

object BasicSliceAdapters {
    
    private fun readRaw(element: Element, css: String, attr: String): String? {
        var el = element
        if (css.isNotEmpty()) {
            el = el.selectFirst(css) ?: return null
        }
        
        return when (attr) {
            Attrs.TEXT -> el.text()
            Attrs.OWN_TEXT -> el.ownText()
            Attrs.HTML -> el.html()
            Attrs.OUTER_HTML -> el.outerHtml()
            else -> el.attr(attr)
        }
    }

    val STRING_ADAPTER = object : SliceAdapter<String> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): String {
            val targetAttr = if (ownText) Attrs.OWN_TEXT else attr
            return readRaw(element, css, targetAttr) ?: ""
        }
    }

    val INT_ADAPTER = object : SliceAdapter<Int> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Int {
            val targetAttr = if (ownText) Attrs.OWN_TEXT else attr
            return readRaw(element, css, targetAttr)?.toIntOrNull() ?: 0
        }
    }

    val LONG_ADAPTER = object : SliceAdapter<Long> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Long {
            val targetAttr = if (ownText) Attrs.OWN_TEXT else attr
            return readRaw(element, css, targetAttr)?.toLongOrNull() ?: 0L
        }
    }

    val FLOAT_ADAPTER = object : SliceAdapter<Float> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Float {
            val targetAttr = if (ownText) Attrs.OWN_TEXT else attr
            return readRaw(element, css, targetAttr)?.toFloatOrNull() ?: 0.0f
        }
    }

    val BOOLEAN_ADAPTER = object : SliceAdapter<Boolean> {
        override fun read(element: Element, css: String, attr: String, ownText: Boolean): Boolean {
            val targetAttr = if (ownText) Attrs.OWN_TEXT else attr
            val raw = readRaw(element, css, targetAttr) ?: return false
            return raw.lowercase().let { it == "true" || it == "1" || it == "yes" }
        }
    }
}
