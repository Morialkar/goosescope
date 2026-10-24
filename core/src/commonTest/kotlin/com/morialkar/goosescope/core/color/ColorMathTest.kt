package com.morialkar.goosescope.core.color

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorMathTest {
    private fun assertOklab(expected: Oklab, actual: Oklab, tol: Double = 1e-3) {
        assertTrue(
            abs(expected.l - actual.l) < tol && abs(expected.a - actual.a) < tol && abs(expected.b - actual.b) < tol,
            "attendu $expected, obtenu $actual",
        )
    }

    @Test
    fun `matches Ottosson reference values`() {
        assertOklab(Oklab(1.0, 0.0, 0.0), ColorMath.toOklab(Srgb8.WHITE))
        assertOklab(Oklab(0.0, 0.0, 0.0), ColorMath.toOklab(Srgb8.BLACK))
        assertOklab(Oklab(0.62796, 0.22486, 0.12585), ColorMath.toOklab(Srgb8(255, 0, 0)))
        assertOklab(Oklab(0.86644, -0.23389, 0.17950), ColorMath.toOklab(Srgb8(0, 255, 0)))
        assertOklab(Oklab(0.45201, -0.03246, -0.31153), ColorMath.toOklab(Srgb8(0, 0, 255)))
    }

    @Test
    fun `round trips every color of a 16 cube within one step`() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val c = Srgb8(r, g, b)
            val back = ColorMath.toSrgb8(ColorMath.toOklab(c))
            assertTrue(abs(back.r - r) <= 1 && abs(back.g - g) <= 1 && abs(back.b - b) <= 1, "$c -> $back")
        }
    }

    @Test
    fun `hue is normalized to 0 until 360`() {
        val blue = ColorMath.toOklch(ColorMath.toOklab(Srgb8(0, 0, 255)))
        assertTrue(blue.hDeg in 0.0..360.0)
        assertEquals(264.05, blue.hDeg, 0.1)
        // Un rose-rouge a une teinte juste sous 360°, jamais négative.
        val magentaRed = ColorMath.toOklch(ColorMath.toOklab(Srgb8(255, 0, 128)))
        assertTrue(magentaRed.hDeg in 0.0..360.0)
    }

    @Test
    fun `gray has no chroma`() {
        val gray = ColorMath.toOklch(ColorMath.toOklab(Srgb8(128, 128, 128)))
        assertTrue(gray.c < 1e-4)
    }

    @Test
    fun `out of gamut colors are clamped`() {
        assertEquals(Srgb8.WHITE, ColorMath.toSrgb8(LinearRgb(1.5, 2.0, 1.2)))
        assertEquals(Srgb8.BLACK, ColorMath.toSrgb8(LinearRgb(-0.2, -1.0, 0.0)))
    }

    @Test
    fun `distance is zero for identical colors and symmetric`() {
        val x = ColorMath.toOklab(Srgb8(10, 120, 200))
        val y = ColorMath.toOklab(Srgb8(200, 120, 10))
        assertEquals(0.0, ColorMath.distance(x, x))
        assertEquals(ColorMath.distance(x, y), ColorMath.distance(y, x))
    }
}
