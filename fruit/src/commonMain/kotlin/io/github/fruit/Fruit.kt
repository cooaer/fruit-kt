package io.github.fruit

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.github.fruit.annotations.Pick
import io.github.fruit.annotations.Attrs
import kotlin.reflect.KClass

/**
 * Kotlin Multiplatform HTML to Object Binder
 */
class Fruit(
    private val factories: List<PickAdapterFactory> = emptyList()
) {
    private val adapterCache = mutableMapOf<KClass<*>, PickAdapter<*>>()

    fun <T : Any> registerAdapter(clazz: KClass<T>, adapter: PickAdapter<T>) {
        adapterCache[clazz] = adapter
    }

    fun <T : Any> fromHtml(html: String, clazz: KClass<T>): T? {
        if (html.isEmpty()) return null
        return fromHtml(Ksoup.parse(html), clazz)
    }

    fun <T : Any> fromHtml(element: Element, clazz: KClass<T>): T? {
        val adapter = getAdapter(clazz)
        return adapter.read(element)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAdapter(clazz: KClass<T>): PickAdapter<T> {
        val cached = adapterCache[clazz]
        if (cached != null) return cached as PickAdapter<T>

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

interface PickAdapter<T> {
    fun read(element: Element, css: String = "", attr: String = Attrs.TEXT, ownText: Boolean = false): T?
}

interface PickAdapterFactory {
    fun <T> create(fruit: Fruit, type: Any): PickAdapter<T>?
}
