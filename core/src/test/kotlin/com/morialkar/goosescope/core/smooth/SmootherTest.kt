package com.morialkar.goosescope.core.smooth

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.classify.BaseClassifier
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SmootherTest {
    private val classifier = BaseClassifier(Catalog.loadBundled())
    private val frameMs = 33L

    private fun lab(hex: String) = ColorMath.toOklab(Srgb8.fromHex(hex))

    private fun lerp(x: Oklab, y: Oklab, t: Double) =
        Oklab(x.l + (y.l - x.l) * t, x.a + (y.a - x.a) * t, x.b + (y.b - x.b) * t)

    /** Paramètre t où la classification bascule entre x et y, trouvé par dichotomie. */
    private fun frontier(x: Oklab, y: Oklab): Double {
        val bx = classifier.classify(x).base
        var lo = 0.0
        var hi = 1.0
        repeat(40) {
            val mid = (lo + hi) / 2
            if (classifier.classify(lerp(x, y, mid)).base == bx) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    @Test
    fun `stable color is named from the first reading`() {
        val s = Smoother(classifier)
        repeat(30) { i -> assertEquals(BaseColor.BLUE, s.push(lab("#1E90FF"), i * frameMs).base) }
    }

    @Test
    fun `oscillating at a frontier changes the name at most once per second`() {
        // AC4: 5 s à 30 i/s, bruit de part et d'autre d'une frontière rouge/orange.
        val red = lab("#E02020")
        val orange = lab("#FF8800")
        val t = frontier(red, orange)
        val below = lerp(red, orange, t - 0.04)
        val above = lerp(red, orange, t + 0.04)
        val s = Smoother(classifier)
        var changes = 0
        var last: BaseColor? = null
        for (i in 0 until 150) {
            val base = s.push(if (i % 2 == 0) below else above, i * frameMs).base
            if (last != null && base != last) changes++
            last = base
        }
        assertTrue(changes <= 5, "$changes changements en 5 s")
    }

    @Test
    fun `a clear change is shown within 300 ms`() {
        val s = Smoother(classifier)
        repeat(30) { i -> s.push(lab("#1E90FF"), i * frameMs) }
        val start = 30 * frameMs
        var i = 0
        while (s.push(lab("#228B22"), start + i * frameMs).base != BaseColor.GREEN) i++
        assertTrue(i * frameMs <= 300, "vert affiché après ${i * frameMs} ms")
    }

    @Test
    fun `reset forgets the previous color`() {
        val s = Smoother(classifier)
        repeat(10) { i -> s.push(lab("#1E90FF"), i * frameMs) }
        s.reset()
        assertEquals(BaseColor.RED, s.push(lab("#E02020"), 0).base)
    }

    @Test
    fun `rejects a timestamp going backwards`() {
        val s = Smoother(classifier)
        s.push(lab("#1E90FF"), 100)
        assertFailsWith<IllegalArgumentException> { s.push(lab("#1E90FF"), 99) }
    }

    @Test
    fun `rejects invalid parameters`() {
        assertFailsWith<IllegalArgumentException> { Smoother(classifier, alpha = 0.0) }
        assertFailsWith<IllegalArgumentException> { Smoother(classifier, margin = -1.0) }
    }
}
