package io.github.fruit.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pick(
    val value: String = "",
    val attr: String = Attrs.TEXT,
    val ownText: Boolean = false
)

object Attrs {
    const val TEXT = "text"
    const val OWN_TEXT = "ownText"
    const val HTML = "html"
    const val OUTER_HTML = "outerHtml"
    const val SRC = "src"
    const val HREF = "href"
}
