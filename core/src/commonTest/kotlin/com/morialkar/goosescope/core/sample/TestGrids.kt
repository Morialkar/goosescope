package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8

class FunctionGrid(override val width: Int, override val height: Int, val f: (Int, Int) -> Srgb8) : PixelGrid {
    var reads = 0
    override fun rgbAt(x: Int, y: Int): Srgb8 {
        check(x in 0 until width && y in 0 until height) { "Accès hors limites: ($x, $y)" }
        reads++
        return f(x, y)
    }
}

fun uniform(w: Int, h: Int, c: Srgb8) = FunctionGrid(w, h) { _, _ -> c }
