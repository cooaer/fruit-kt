package io.github.fruit.sample.ksp

import io.github.fruit.RawResponseHolder
import io.github.fruit.annotations.Pick
import io.github.fruit.annotations.Pulp

@Pulp(".article")
data class GeneratedNews(
    override val rawResponse: String = "",
    @property:Pick("h1")
    val title: String = "",
    @property:Pick("a", attr = "href")
    val link: String = "",
    @property:Pick(".tags .tag")
    val tags: List<Tag> = emptyList(),
    @property:Pick(".meta")
    val meta: Meta? = null,
) : RawResponseHolder {

    @Pulp
    data class Tag(
        @property:Pick
        val name: String = ""
    )

    @Pulp
    data class Meta(
        @property:Pick(".author")
        val author: String = ""
    )
}
