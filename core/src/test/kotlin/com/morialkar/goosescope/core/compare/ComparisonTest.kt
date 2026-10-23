package com.morialkar.goosescope.core.compare

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.catalog.Lang
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComparisonTest {
    private val catalog = Catalog.loadBundled()
    private fun names(lang: Lang): (BaseColor) -> String = { catalog.base(it).name[lang] }
    private fun lab(hex: String) = ColorMath.toOklab(Srgb8.fromHex(hex))

    private val peach = lab("#FFCBA4")
    // Pêche tirée vers le rouge: même clarté environ, teinte plus basse.
    private val redderPeach = lab("#FFBBA4")

    @Test
    fun `peach shifted toward red mentions red and warmth`() {
        val f = ComparisonEngine.compare(redderPeach, peach)
        assertEquals(BaseColor.RED, f.hueShiftToward)
        assertEquals(TemperatureShift.WARMER, f.temperature)
        val en = ComparisonText.render(f, "Peach", Lang.EN, names(Lang.EN))
        val fr = ComparisonText.render(f, "Pêche", Lang.FR, names(Lang.FR))
        assertTrue("more red" in en && "warmer" in en, en)
        assertTrue("plus de rouge" in fr && "plus chaleureuse" in fr, fr)
    }

    @Test
    fun `negligible difference reads as very close`() {
        val f = ComparisonEngine.compare(lab("#FFCAA4"), peach)
        assertEquals(Closeness.VERY_CLOSE, f.closeness)
        assertEquals("This shade is very close to Peach.", ComparisonText.render(f, "Peach", Lang.EN, names(Lang.EN)))
        assertEquals("Cette teinte est très proche de la nuance Pêche.", ComparisonText.render(f, "Pêche", Lang.FR, names(Lang.FR)))
    }

    @Test
    fun `hue of near grays is not mentioned`() {
        val f = ComparisonEngine.compare(lab("#8A8580"), lab("#80858A"))
        assertNull(f.hueShiftToward)
        assertEquals(TemperatureShift.SAME, f.temperature)
    }

    @Test
    fun `darker and paler are detected`() {
        val f = ComparisonEngine.compare(lab("#A08060"), peach)
        assertEquals(LightnessShift.DARKER, f.lightness)
        assertEquals(Closeness.CLEAR, f.closeness)
        val fr = ComparisonText.render(f, "Pêche", Lang.FR, names(Lang.FR))
        assertTrue("plus foncée" in fr, fr)
    }

    @Test
    fun `french elides before a vowel`() {
        val f = ComparisonFacts(Closeness.SLIGHT, BaseColor.ORANGE, LightnessShift.SAME, ChromaShift.SAME, TemperatureShift.SAME)
        assertEquals(
            "Par rapport à la nuance Rouille, cette teinte contient légèrement plus d'orange.",
            ComparisonText.render(f, "Rouille", Lang.FR, names(Lang.FR)),
        )
    }

    @Test
    fun `hue delta wraps around 360`() {
        assertEquals(20.0, ComparisonEngine.signedHueDelta(350.0, 10.0), 1e-9)
        assertEquals(-20.0, ComparisonEngine.signedHueDelta(10.0, 350.0), 1e-9)
    }

    @Test
    fun `every combination renders a complete sentence in both languages`() {
        for (c in Closeness.entries) for (h in listOf<BaseColor?>(null) + BaseColor.entries)
            for (l in LightnessShift.entries) for (ch in ChromaShift.entries) for (t in TemperatureShift.entries)
                for (lang in Lang.entries) {
                    val s = ComparisonText.render(ComparisonFacts(c, h, l, ch, t), "Shade", lang, names(lang))
                    assertTrue(s.isNotBlank() && s.endsWith(".") && '{' !in s && "null" !in s, s)
                }
    }

    @Test
    fun `rejects non finite colors`() {
        assertFailsWith<IllegalArgumentException> { ComparisonEngine.compare(Oklab(Double.NaN, 0.0, 0.0), peach) }
    }
}
