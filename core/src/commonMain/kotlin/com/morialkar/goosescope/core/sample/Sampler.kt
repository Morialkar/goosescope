package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.Tuning
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.LinearRgb
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.math.max
import kotlin.math.roundToInt

object Sampler {

    /**
     * Moyenne d'un carré centré sur [center] (SPEC F3). La moyenne se fait en lumière linéaire:
     * en sRGB, un mélange noir/blanc donnerait un gris trop foncé. Le carré est rogné aux bords.
     */
    fun sample(grid: PixelGrid, center: Point, sideFraction: Double = Tuning.SAMPLE_SIDE_FRACTION): Srgb8 {
        require(sideFraction > 0.0 && sideFraction <= 1.0) { "sideFraction hors de ]0, 1]: $sideFraction" }
        require(center.x in 0 until grid.width && center.y in 0 until grid.height) { "Point hors de l'image: $center" }
        val half = max(1, (minOf(grid.width, grid.height) * sideFraction).roundToInt()) / 2
        val x0 = max(0, center.x - half)
        val x1 = minOf(grid.width - 1, center.x + half)
        val y0 = max(0, center.y - half)
        val y1 = minOf(grid.height - 1, center.y + half)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        for (y in y0..y1) for (x in x0..x1) {
            val lin = ColorMath.toLinear(grid.rgbAt(x, y))
            r += lin.r
            g += lin.g
            b += lin.b
        }
        val n = ((x1 - x0 + 1) * (y1 - y0 + 1)).toDouble()
        return ColorMath.toSrgb8(LinearRgb(r / n, g / n, b / n))
    }

    fun sampleCenter(grid: PixelGrid): Srgb8 = sample(grid, Point(grid.width / 2, grid.height / 2))

    /** Luminance relative moyenne de toute l'image, sous-échantillonnée (ARCHITECTURE §3.4). */
    fun meanLuminance(grid: PixelGrid, step: Int = Tuning.LUMINANCE_STEP): Double {
        require(step >= 1) { "step doit être ≥ 1: $step" }
        var sum = 0.0
        var n = 0
        for (y in 0 until grid.height step step) for (x in 0 until grid.width step step) {
            val lin = ColorMath.toLinear(grid.rgbAt(x, y))
            sum += 0.2126 * lin.r + 0.7152 * lin.g + 0.0722 * lin.b
            n++
        }
        return if (n == 0) 0.0 else sum / n
    }
}
