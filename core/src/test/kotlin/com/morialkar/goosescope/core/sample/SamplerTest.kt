package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SamplerTest {
    @Test
    fun `uniform grid samples to its color`() {
        val c = Srgb8.fromHex("#FFCBA4")
        assertEquals(c, Sampler.sampleCenter(uniform(1280, 720, c)))
    }

    @Test
    fun `averages in linear light`() {
        // Damier noir/blanc: la moyenne linéaire donne ≈ #BCBCBC, pas #808080.
        val grid = FunctionGrid(200, 200) { x, y -> if ((x + y) % 2 == 0) Srgb8.BLACK else Srgb8.WHITE }
        val s = Sampler.sample(grid, Point(100, 100), sideFraction = 0.1)
        assertTrue(s.r in 185..192, "obtenu ${s.toHex()}")
    }

    @Test
    fun `sampling at a corner stays in bounds`() {
        val grid = uniform(100, 100, Srgb8(10, 20, 30))
        assertEquals(Srgb8(10, 20, 30), Sampler.sample(grid, Point(0, 0), sideFraction = 0.5))
        assertEquals(Srgb8(10, 20, 30), Sampler.sample(grid, Point(99, 99), sideFraction = 0.5))
    }

    @Test
    fun `tiny images still sample one pixel`() {
        assertEquals(Srgb8(1, 2, 3), Sampler.sampleCenter(uniform(1, 1, Srgb8(1, 2, 3))))
    }

    @Test
    fun `rejects invalid side fraction or point`() {
        val grid = uniform(10, 10, Srgb8.BLACK)
        assertFailsWith<IllegalArgumentException> { Sampler.sample(grid, Point(5, 5), 0.0) }
        assertFailsWith<IllegalArgumentException> { Sampler.sample(grid, Point(5, 5), 1.5) }
        assertFailsWith<IllegalArgumentException> { Sampler.sample(grid, Point(10, 5)) }
    }

    @Test
    fun `mean luminance of white is one and of black is zero`() {
        assertEquals(1.0, Sampler.meanLuminance(uniform(64, 64, Srgb8.WHITE)), 1e-9)
        assertEquals(0.0, Sampler.meanLuminance(uniform(64, 64, Srgb8.BLACK)), 1e-9)
        assertFailsWith<IllegalArgumentException> { Sampler.meanLuminance(uniform(4, 4, Srgb8.BLACK), 0) }
    }

    @Test
    fun `mean luminance subsamples the image`() {
        val grid = uniform(800, 800, Srgb8.WHITE)
        Sampler.meanLuminance(grid, step = 8)
        assertEquals(100 * 100, grid.reads)
    }
}
