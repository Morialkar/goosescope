package com.morialkar.goosescope.core.smooth

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.classify.ShadeMatcher
import com.morialkar.goosescope.core.color.Oklab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShadeStabilizerTest {
    private val catalog = Catalog.loadBundled()
    private val matcher = ShadeMatcher(catalog)
    private fun ref(id: String) = catalog.shades.first { it.id == id }

    private fun lerp(x: Oklab, y: Oklab, t: Double) =
        Oklab(x.l + (y.l - x.l) * t, x.a + (y.a - x.a) * t, x.b + (y.b - x.b) * t)

    @Test
    fun `a reference color shows its own shade`() {
        val s = ShadeStabilizer(matcher)
        assertEquals("peach", s.update(ref("peach").refLab, BaseColor.ORANGE).id)
    }

    @Test
    fun `oscillating between two shades of the same base does not flicker`() {
        // AC17: va-et-vient autour du milieu entre Pêche et Abricot.
        val peach = ref("peach").refLab
        val apricot = ref("apricot").refLab
        val s = ShadeStabilizer(matcher)
        val shown = (0 until 60).map { i ->
            s.update(lerp(peach, apricot, if (i % 2 == 0) 0.47 else 0.53), BaseColor.ORANGE).id
        }
        assertEquals(1, shown.toSet().size, "nuances affichées: ${shown.toSet()}")
    }

    @Test
    fun `a clear move to another shade is shown`() {
        val s = ShadeStabilizer(matcher)
        s.update(ref("peach").refLab, BaseColor.ORANGE)
        assertEquals("pumpkin", s.update(ref("pumpkin").refLab, BaseColor.ORANGE).id)
    }

    @Test
    fun `a base change switches the shade immediately`() {
        val s = ShadeStabilizer(matcher)
        s.update(ref("peach").refLab, BaseColor.ORANGE)
        val shade = s.update(ref("sky_blue").refLab, BaseColor.BLUE)
        assertEquals(BaseColor.BLUE, shade.base)
        assertEquals("sky_blue", shade.id)
    }

    @Test
    fun `the shown shade always belongs to the requested base`() {
        val s = ShadeStabilizer(matcher)
        s.update(ref("peach").refLab, BaseColor.ORANGE)
        // Couleur de pêche, mais la base stabilisée est devenue rose: la nuance suit la base.
        assertTrue(s.update(ref("peach").refLab, BaseColor.PINK).base == BaseColor.PINK)
    }

    @Test
    fun `reset forgets the shown shade`() {
        val s = ShadeStabilizer(matcher, margin = 1.0)
        s.update(ref("peach").refLab, BaseColor.ORANGE)
        s.reset()
        assertEquals("pumpkin", s.update(ref("pumpkin").refLab, BaseColor.ORANGE).id)
    }

    @Test
    fun `rejects a negative margin`() {
        assertFailsWith<IllegalArgumentException> { ShadeStabilizer(matcher, margin = -0.1) }
    }
}
