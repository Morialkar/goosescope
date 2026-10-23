package com.morialkar.goosescope.core.classify

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShadeMatcherTest {
    // Fixture: peach (orange), sky_blue (blue), forest_green (green).
    private val catalog = Catalog.load(checkNotNull(javaClass.getResourceAsStream("/fixtures/valid.json")).bufferedReader().readText())
    private val matcher = ShadeMatcher(catalog)
    private fun lab(hex: String) = ColorMath.toOklab(Srgb8.fromHex(hex))

    @Test
    fun `a reference color matches its own shade`() {
        assertEquals("peach", matcher.match(lab("#FFCBA4"), BaseColor.ORANGE).id)
        assertEquals("sky_blue", matcher.match(lab("#87CEEB"), BaseColor.BLUE).id)
    }

    @Test
    fun `stays within the requested base even when another shade is closer`() {
        // Ce vert est bien plus proche de forest_green, mais la base demandée est le bleu.
        assertEquals("sky_blue", matcher.match(lab("#228B22"), BaseColor.BLUE).id)
    }

    @Test
    fun `fails clearly when a base has no shade`() {
        val e = assertFailsWith<IllegalStateException> { matcher.match(lab("#FF0000"), BaseColor.RED) }
        assertEquals("Aucune nuance pour la base red dans le catalogue", e.message)
    }
}
