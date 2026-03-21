package io.github.fruit.annotations

/**
 * Marks a class for HTML adapter generation.
 * This is useful when you want to generate an adapter for a class without
 * using a class-level @Pick annotation (e.g. relying only on field-level @Pick or just type mapping).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pulp(
    val value: String = ""
)
