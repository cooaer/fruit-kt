package io.github.fruit.annotations

/**
 * Marks a class as a sliceable HTML scope for generated adapter creation.
 * The optional value narrows parsing to a root CSS selector before
 * property-level @Pick rules are applied.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Slice(
    val value: String = ""
)
