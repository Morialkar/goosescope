package com.morialkar.goosescope.core.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {
    @Test
    fun `black on white is 21 to 1`() {
        assertEquals(21.0, Contrast.ratio(Srgb8.BLACK, Srgb8.WHITE), 1e-9)
        assertEquals(1.0, Contrast.ratio(Srgb8.WHITE, Srgb8.WHITE), 1e-9)
    }

    @Test
    fun `picks black on light colors and white on dark colors`() {
        assertEquals(Srgb8.BLACK, Contrast.textColorOn(Srgb8.fromHex("#FFCBA4")))
        assertEquals(Srgb8.WHITE, Contrast.textColorOn(Srgb8.fromHex("#1F2328")))
    }

    @Test
    fun `worst mid tone still meets AA`() {
        // Luminance ≈ 0,18: point où noir et blanc offrent le même contraste (≈ 4,58).
        val mid = Srgb8(118, 118, 118)
        val c = Contrast.ratio(mid, Contrast.textColorOn(mid))
        assertTrue(c in 4.5..4.7, "contraste $c")
    }

    @Test
    fun `text on any color of a 16 cube meets AA`() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val bg = Srgb8(r, g, b)
            val c = Contrast.ratio(bg, Contrast.textColorOn(bg))
            assertTrue(c >= 4.5, "${bg.toHex()} -> $c")
        }
    }

    @Test
    fun `theme tokens meet AA`() {
        val text = Srgb8.fromHex("#F4F2EC")
        assertTrue(Contrast.ratio(text, Srgb8.fromHex("#3F4752")) >= 4.5)
        assertTrue(Contrast.ratio(text, Srgb8.fromHex("#545D69")) >= 4.5)
        // textMuted, sur surface et sur surfaceRaised (étiquettes de la fiche).
        assertTrue(Contrast.ratio(Srgb8.fromHex("#D8DBDF"), Srgb8.fromHex("#3F4752")) >= 4.5)
        assertTrue(Contrast.ratio(Srgb8.fromHex("#D8DBDF"), Srgb8.fromHex("#545D69")) >= 4.5)
    }
}
