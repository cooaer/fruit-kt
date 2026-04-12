package io.github.fruit.sample.ksp

import io.github.fruit.annotations.Pick
import io.github.fruit.annotations.Pulp

@Pulp(".article")
class GeneratedNews {
    @Pick("h1")
    var title: String = ""

    @Pick("a", attr = "href")
    var link: String = ""
}
