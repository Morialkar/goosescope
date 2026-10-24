package com.morialkar.goosescope.core.catalog

import kotlinx.serialization.Serializable

// Forme brute de catalog.json (ARCHITECTURE §4.1). Validée puis convertie par Catalog.load.

@Serializable
internal data class TextJson(val en: String, val fr: String)

@Serializable
internal data class ListJson(val en: List<String>, val fr: List<String>)

@Serializable
internal data class BaseJson(val id: String, val name: TextJson, val anchors: List<String>)

@Serializable
internal data class ShadeJson(
    val id: String,
    val base: String,
    val ref: String,
    val name: TextJson,
    val description: TextJson,
    val nature: ListJson,
)

@Serializable
internal data class CatalogJson(val version: Int, val bases: List<BaseJson>, val shades: List<ShadeJson>)
