package com.morialkar.goosescope.core.catalog

import com.morialkar.goosescope.core.classify.BaseClassifier
import com.morialkar.goosescope.core.classify.ShadeMatcher
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// AC9 et AC13: règles de quantité et de cohérence du catalogue livré (PLAN T09).
class BundledCatalogTest {
    private val catalog = Catalog.loadBundled()
    private val classifier = BaseClassifier(catalog)
    private val matcher = ShadeMatcher(catalog)

    @Test
    fun `has between 100 and 150 shades`() {
        assertTrue(catalog.shades.size in 100..150, "${catalog.shades.size} nuances")
    }

    @Test
    fun `every base has enough shades`() {
        BaseColor.entries.forEach { base ->
            val n = catalog.shadesOf(base).size
            val min = if (base.isNeutral) 2 else 5
            assertTrue(n >= min, "${base.id}: $n nuances, minimum $min")
        }
    }

    @Test
    fun `every reference color is classified in its own base`() {
        val wrong = catalog.shades.filter { classifier.classify(it.refLab).base != it.base }
            .map { "${it.id} ${it.ref.toHex()} -> ${classifier.classify(it.refLab).base}" }
        assertTrue(wrong.isEmpty(), "Mal rattachées: $wrong")
    }

    @Test
    fun `every reference color matches its own shade`() {
        catalog.shades.forEach { assertEquals(it.id, matcher.match(it.refLab, it.base).id) }
    }

    @Test
    fun `shade and base always agree across the color space`() {
        // AC13: grille 32³ de tout l'espace sRGB.
        for (r in 0..255 step 8) for (g in 0..255 step 8) for (b in 0..255 step 8) {
            val c = ColorMath.toOklab(Srgb8(r, g, b))
            val base = classifier.classify(c).base
            assertEquals(base, matcher.match(c, base).base)
        }
    }

    @Test
    fun `display names are unique within each language`() {
        Lang.entries.forEach { lang ->
            val dups = catalog.shades.groupBy { it.name[lang] }.filter { it.value.size > 1 }.keys
            assertTrue(dups.isEmpty(), "$lang: noms en double $dups")
        }
    }
}
