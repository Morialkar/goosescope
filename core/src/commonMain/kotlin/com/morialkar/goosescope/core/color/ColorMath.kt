@file:OptIn(ExperimentalObjCName::class)

package com.morialkar.goosescope.core.color

import kotlin.experimental.ExperimentalObjCName
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.native.ObjCName

// ARCHITECTURE §3.1. Les surcharges reçoivent un nom Swift explicite: sans cela, Swift les distingue
// par l'ordre de déclaration (`toOklab(c:)` / `toOklab(c_:)`), ce qui casserait au moindre déplacement.
// Matrices OKLab de Björn Ottosson (https://bottosson.github.io/posts/oklab/).
object ColorMath {

    @ObjCName(swiftName = "linearFromSrgb")
    fun toLinear(c: Srgb8): LinearRgb = LinearRgb(decode(c.r), decode(c.g), decode(c.b))

    /** Borne chaque canal à 0..1 avant d'encoder: les couleurs hors gamut sont ramenées au plus proche. */
    @ObjCName(swiftName = "srgbFromLinear")
    fun toSrgb8(c: LinearRgb): Srgb8 = Srgb8(encode(c.r), encode(c.g), encode(c.b))

    @ObjCName(swiftName = "oklabFromSrgb")
    fun toOklab(c: Srgb8): Oklab = toOklab(toLinear(c))

    @ObjCName(swiftName = "oklabFromLinear")
    fun toOklab(c: LinearRgb): Oklab {
        val l = cbrt(0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b)
        val m = cbrt(0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b)
        val s = cbrt(0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b)
        return Oklab(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    @ObjCName(swiftName = "linearFromOklab")
    fun toLinear(c: Oklab): LinearRgb {
        val l = (c.l + 0.3963377774 * c.a + 0.2158037573 * c.b).pow(3)
        val m = (c.l - 0.1055613458 * c.a - 0.0638541728 * c.b).pow(3)
        val s = (c.l - 0.0894841775 * c.a - 1.2914855480 * c.b).pow(3)
        return LinearRgb(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    @ObjCName(swiftName = "srgbFromOklab")
    fun toSrgb8(c: Oklab): Srgb8 = toSrgb8(toLinear(c))

    fun toOklch(c: Oklab): Oklch {
        val h = atan2(c.b, c.a) * 180.0 / PI
        return Oklch(c.l, sqrt(c.a * c.a + c.b * c.b), if (h < 0) h + 360.0 else h)
    }

    fun distance(x: Oklab, y: Oklab): Double {
        val dl = x.l - y.l
        val da = x.a - y.a
        val db = x.b - y.b
        return sqrt(dl * dl + da * da + db * db)
    }

    private fun decode(channel: Int): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun encode(linear: Double): Int {
        val v = linear.coerceIn(0.0, 1.0)
        val s = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
        return (s * 255).roundToInt().coerceIn(0, 255)
    }
}
