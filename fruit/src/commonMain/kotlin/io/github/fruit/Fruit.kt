package io.github.fruit

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.annotations.Attrs
import kotlin.reflect.KClass

/**
 * Kotlin Multiplatform HTML to Object Binder
 */
class Fruit(
    private val factories: List<SliceAdapterFactory> = emptyList()
) {
    private val adapterCache = mutableMapOf<KClass<*>, SliceAdapter<*>>()

    fun <T : Any> registerSliceAdapter(clazz: KClass<T>, adapter: SliceAdapter<T>) {
        adapterCache[clazz] = adapter
    }

    fun <T : Any> fromHtml(html: String, clazz: KClass<T>): T? {
        if (html.isEmpty()) return null
        return fromHtml(Ksoup.parse(html), clazz)
    }

    fun <T : Any> fromHtml(element: Element, clazz: KClass<T>): T? {
        val adapter = getSliceAdapter(clazz)
        return adapter.read(element)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSliceAdapter(clazz: KClass<T>): SliceAdapter<T> {
        val cached = adapterCache[clazz]
        if (cached != null) return cached as SliceAdapter<T>

        for (factory in factories) {
            val adapter = factory.create<T>(this, clazz)
            if (adapter != null) {
                adapterCache[clazz] = adapter
                return adapter
            }
        }
        
        throw IllegalArgumentException("Fruit cannot handle $clazz. No factory found.")
    }

    companion object {
        fun createDefault(): Fruit {
            return Fruit()
        }
    }
}

interface SliceAdapter<T> {
    fun read(element: Element, css: String = "", attr: String = Attrs.TEXT, ownText: Boolean = false): T?
}

interface SliceAdapterFactory {
    fun <T> create(fruit: Fruit, type: Any): SliceAdapter<T>?
}

interface RawResponseHolder {
    val rawResponse: String
}
