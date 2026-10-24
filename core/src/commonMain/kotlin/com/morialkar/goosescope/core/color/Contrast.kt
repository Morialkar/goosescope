package com.morialkar.goosescope.core.color

// ARCHITECTURE §3.7, STANDARDS §4: contraste WCAG 2.x.
object Contrast {

    fun relativeLuminance(c: Srgb8): Double {
        val lin = ColorMath.toLinear(c)
        return 0.2126 * lin.r + 0.7152 * lin.g + 0.0722 * lin.b
    }

    fun ratio(x: Srgb8, y: Srgb8): Double {
        val a = relativeLuminance(x)
        val b = relativeLuminance(y)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /** Noir ou blanc purs: seule paire qui garantit ≥ 4,5:1 sur toute couleur (STANDARDS §4). */
    fun textColorOn(background: Srgb8): Srgb8 =
        if (ratio(background, Srgb8.BLACK) >= ratio(background, Srgb8.WHITE)) Srgb8.BLACK else Srgb8.WHITE
}
