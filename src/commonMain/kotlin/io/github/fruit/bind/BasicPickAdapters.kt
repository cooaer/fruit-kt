package io.github.fruit.bind

import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.PickAdapter
import io.github.fruit.annotations.Attrs
import io.github.fruit.annotations.Pick

object BasicPickAdapters {
    
    private fun readRaw(element: Element, pick: Pick?): String? {
        var el = element
        if (pick != null && pick.value.isNotEmpty()) {
            el = el.selectFirst(pick.value) ?: return null
        }
        
        val attrName = pick?.attr ?: Attrs.TEXT
        return when (attrName) {
            Attrs.TEXT -> if (pick?.ownText == true) el.ownText() else el.text()
            Attrs.HTML -> el.html()
            Attrs.OUTER_HTML -> el.outerHtml()
            else -> el.attr(attrName)
        }
    }

    val STRING_ADAPTER = object : PickAdapter<String> {
        override fun read(element: Element, pick: Pick?): String = readRaw(element, pick) ?: ""
    }

    val INT_ADAPTER = object : PickAdapter<Int> {
        override fun read(element: Element, pick: Pick?): Int = readRaw(element, pick)?.toIntOrNull() ?: 0
    }

    val LONG_ADAPTER = object : PickAdapter<Long> {
        override fun read(element: Element, pick: Pick?): Long = readRaw(element, pick)?.toLongOrNull() ?: 0L
    }

    val FLOAT_ADAPTER = object : PickAdapter<Float> {
        override fun read(element: Element, pick: Pick?): Float = readRaw(element, pick)?.toFloatOrNull() ?: 0.0f
    }

    val BOOLEAN_ADAPTER = object : PickAdapter<Boolean> {
        override fun read(element: Element, pick: Pick?): Boolean {
            val raw = readRaw(element, pick) ?: return false
            return raw.lowercase().let { it == "true" || it == "1" || it == "yes" }
        }
    }
}
