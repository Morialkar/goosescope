package com.morialkar.goosescope.core.catalog

import com.morialkar.goosescope.core.color.Oklab
import com.morialkar.goosescope.core.color.Srgb8

enum class Lang { EN, FR }

/** Les 11 couleurs de base de la couche 1 (SPEC F13). `id` est la clé utilisée dans catalog.json. */
enum class BaseColor(val id: String) {
    RED("red"), ORANGE("orange"), YELLOW("yellow"), GREEN("green"), BLUE("blue"), PURPLE("purple"),
    PINK("pink"), BROWN("brown"), GRAY("gray"), BLACK("black"), WHITE("white");

    val isNeutral: Boolean get() = this == GRAY || this == BLACK || this == WHITE

    companion object {
        fun fromId(id: String): BaseColor? = entries.firstOrNull { it.id == id }
    }
}

data class Localized<T>(val en: T, val fr: T) {
    operator fun get(lang: Lang): T = when (lang) {
        Lang.EN -> en
        Lang.FR -> fr
    }
}

data class Anchor(val srgb: Srgb8, val lab: Oklab)

data class BaseEntry(val color: BaseColor, val name: Localized<String>, val anchors: List<Anchor>)

data class Shade(
    val id: String,
    val base: BaseColor,
    val ref: Srgb8,
    val refLab: Oklab,
    val name: Localized<String>,
    val description: Localized<String>,
    val nature: Localized<List<String>>,
)
