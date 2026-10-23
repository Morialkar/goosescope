package com.morialkar.goosescope.core.catalog

import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogTest {
    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { name }.bufferedReader().readText()

    private fun violationsOf(name: String): List<String> =
        assertFailsWith<CatalogException> { Catalog.load(fixture(name)) }.violations

    @Test
    fun `loads a valid catalog`() {
        val catalog = Catalog.load(fixture("valid.json"))
        assertEquals(11, catalog.bases.size)
        assertEquals(3, catalog.shades.size)
        val peach = catalog.shades.first { it.id == "peach" }
        assertEquals(BaseColor.ORANGE, peach.base)
        assertEquals(Srgb8.fromHex("#FFCBA4"), peach.ref)
        assertEquals("Pêche", peach.name[Lang.FR])
        assertEquals("Peach", peach.name[Lang.EN])
        assertEquals(listOf(peach), catalog.shadesOf(BaseColor.ORANGE))
    }

    @Test
    fun `accepts exactly three nature items`() {
        val peach = Catalog.load(fixture("valid.json")).shades.first { it.id == "peach" }
        assertEquals(3, peach.nature[Lang.EN].size)
        assertEquals(3, peach.nature[Lang.FR].size)
    }

    @Test
    fun `loads the bundled catalog`() {
        val catalog = Catalog.loadBundled()
        assertEquals(BaseColor.entries.toSet(), catalog.bases.map { it.color }.toSet())
    }

    @Test
    fun `rejects an unknown base`() {
        assertTrue(violationsOf("unknown-base.json").any { "base inconnue \"turquoise\"" in it })
    }

    @Test
    fun `rejects a duplicate id`() {
        assertTrue(violationsOf("duplicate-id.json").any { "nuance en double: peach" in it })
    }

    @Test
    fun `rejects a missing French text`() {
        assertTrue(violationsOf("missing-fr.json").any { "nuance peach: description: texte FR vide" in it })
    }

    @Test
    fun `rejects an invalid hex color`() {
        assertTrue(violationsOf("bad-hex.json").any { "ref invalide \"#FFCBA\"" in it })
    }

    @Test
    fun `rejects a base without anchors`() {
        assertTrue(violationsOf("base-without-anchor.json").any { "base green: aucune ancre" in it })
    }

    @Test
    fun `rejects malformed json`() {
        assertTrue(violationsOf("malformed.json").single().startsWith("JSON illisible"))
    }

    @Test
    fun `lists every violation at once`() {
        val v = violationsOf("many-violations.json")
        assertTrue(v.any { "nature: au moins 3 éléments EN" in it }, v.toString())
        assertTrue(v.any { "snake_case" in it }, v.toString())
        assertTrue(v.any { "ref invalide \"green\"" in it }, v.toString())
        assertEquals(3, v.size, v.toString())
    }
}
