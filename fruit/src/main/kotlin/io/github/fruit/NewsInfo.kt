package io.github.fruit

import io.github.fruit.annotations.Pick

data class NewsInfo(
    @Pick("h1") val title: String = "",
    @Pick("a", attr = "href") val link: String = ""
)
