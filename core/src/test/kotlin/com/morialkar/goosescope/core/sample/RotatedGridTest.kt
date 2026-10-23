package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RotatedGridTest {
    // Image brute 3×2 dont chaque pixel encode sa position: r = x, g = y.
    private val raw = FunctionGrid(3, 2) { x, y -> Srgb8(x, y, 0) }
    private fun at(g: PixelGrid, x: Int, y: Int) = g.rgbAt(x, y).let { it.r to it.g }

    @Test
    fun `no rotation is the identity`() {
        val g = RotatedGrid(raw, 0)
        assertEquals(3 to 2, g.width to g.height)
        assertEquals(2 to 1, at(g, 2, 1))
    }

    @Test
    fun `rotating 90 degrees puts the bottom left raw pixel at the top left`() {
        val g = RotatedGrid(raw, 90)
        assertEquals(2 to 3, g.width to g.height)
        assertEquals(0 to 1, at(g, 0, 0))
        assertEquals(2 to 0, at(g, 1, 2))
    }

    @Test
    fun `rotating 180 and 270 degrees`() {
        assertEquals(2 to 1, at(RotatedGrid(raw, 180), 0, 0))
        val g = RotatedGrid(raw, 270)
        assertEquals(2 to 0, at(g, 0, 0))
        assertEquals(0 to 1, at(g, 1, 2))
    }

    @Test
    fun `every pixel stays in bounds for every rotation`() {
        for (r in listOf(0, 90, 180, 270)) {
            val g = RotatedGrid(raw, r)
            for (y in 0 until g.height) for (x in 0 until g.width) g.rgbAt(x, y)
        }
    }

    @Test
    fun `rejects other angles`() {
        assertFailsWith<IllegalArgumentException> { RotatedGrid(raw, 45) }
    }
}
