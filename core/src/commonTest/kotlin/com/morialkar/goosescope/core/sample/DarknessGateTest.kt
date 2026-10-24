package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DarknessGateTest {
    private val gate = DarknessGate(threshold = 0.02)

    @Test
    fun `covered camera is too dark`() {
        assertTrue(gate.isTooDark(Sampler.meanLuminance(uniform(64, 64, Srgb8(8, 8, 8)))))
    }

    @Test
    fun `black object in a lit scene is not too dark`() {
        // Objet noir au centre, scène éclairée autour.
        val grid = FunctionGrid(200, 200) { x, y -> if (x in 80..120 && y in 80..120) Srgb8.BLACK else Srgb8(180, 170, 160) }
        assertFalse(gate.isTooDark(Sampler.meanLuminance(grid)))
    }

    @Test
    fun `threshold itself is not too dark`() {
        assertFalse(gate.isTooDark(0.02))
        assertTrue(gate.isTooDark(0.0199))
    }

    @Test
    fun `rejects invalid luminance`() {
        assertFailsWith<IllegalArgumentException> { gate.isTooDark(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { gate.isTooDark(-0.1) }
        assertFailsWith<IllegalArgumentException> { gate.isTooDark(1.1) }
    }
}
