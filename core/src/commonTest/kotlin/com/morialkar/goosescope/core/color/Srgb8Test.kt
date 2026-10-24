package com.morialkar.goosescope.core.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Srgb8Test {
    @Test
    fun `parses and prints hex colors`() {
        assertEquals(Srgb8(255, 203, 164), Srgb8.fromHex("#FFCBA4"))
        assertEquals(Srgb8(255, 203, 164), Srgb8.fromHex("#ffcba4"))
        assertEquals("#FFCBA4", Srgb8(255, 203, 164).toHex())
    }

    @Test
    fun `accepts channel limits`() {
        assertEquals("#000000", Srgb8(0, 0, 0).toHex())
        assertEquals("#FFFFFF", Srgb8(255, 255, 255).toHex())
    }

    @Test
    fun `rejects out of range channels`() {
        assertFailsWith<IllegalArgumentException> { Srgb8(256, 0, 0) }
        assertFailsWith<IllegalArgumentException> { Srgb8(0, -1, 0) }
    }

    @Test
    fun `rejects malformed hex`() {
        listOf("#12345", "#GGGGGG", "FFCBA4", "#FFCBA4 ", "").forEach { bad ->
            assertFailsWith<IllegalArgumentException>(bad) { Srgb8.fromHex(bad) }
        }
    }

    @Test
    fun `rejects non finite values`() {
        assertFailsWith<IllegalArgumentException> { LinearRgb(Double.NaN, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { Oklab(0.5, Double.POSITIVE_INFINITY, 0.0) }
    }
}
