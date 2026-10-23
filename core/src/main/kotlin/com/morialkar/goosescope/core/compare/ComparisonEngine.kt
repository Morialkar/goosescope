package com.morialkar.goosescope.core.compare

import com.morialkar.goosescope.core.Tuning
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab
import kotlin.math.abs

// SPEC F15, ARCHITECTURE §3.6.
object ComparisonEngine {

    /**
     * Teinte OKLCh typique de chaque base chromatique, dans l'ordre du cercle. Rose et brun en sont
     * absents: ce sont des variantes de clarté de rouge et d'orange, pas des directions de teinte.
     */
    private val HUE_WHEEL = listOf(
        BaseColor.RED to 29.0, BaseColor.ORANGE to 58.0, BaseColor.YELLOW to 100.0,
        BaseColor.GREEN to 145.0, BaseColor.BLUE to 262.0, BaseColor.PURPLE to 305.0,
    )

    /** Une base voisine doit être au moins à cet écart de la référence pour qu'on dise « vers elle ». */
    private const val MIN_NEIGHBOR_GAP_DEG = 15.0

    fun compare(measured: Oklab, reference: Oklab): ComparisonFacts {
        val m = ColorMath.toOklch(measured)
        val r = ColorMath.toOklch(reference)
        val distance = ColorMath.distance(measured, reference)

        val closeness = when {
            distance < Tuning.COMPARE_VERY_CLOSE -> Closeness.VERY_CLOSE
            distance < Tuning.COMPARE_SLIGHT -> Closeness.SLIGHT
            else -> Closeness.CLEAR
        }
        // La teinte d'une couleur presque grise est instable: on n'en parle que si les deux ont de la couleur.
        val chromatic = m.c > Tuning.COMPARE_MIN_CHROMA && r.c > Tuning.COMPARE_MIN_CHROMA
        val dh = signedHueDelta(r.hDeg, m.hDeg)

        val hueToward = if (chromatic && abs(dh) > Tuning.COMPARE_HUE_DEG) neighbor(r.hDeg, dh > 0) else null
        val lightness = when {
            m.l - r.l > Tuning.COMPARE_LIGHTNESS -> LightnessShift.LIGHTER
            r.l - m.l > Tuning.COMPARE_LIGHTNESS -> LightnessShift.DARKER
            else -> LightnessShift.SAME
        }
        val chroma = when {
            m.c - r.c > Tuning.COMPARE_CHROMA -> ChromaShift.MORE_VIVID
            r.c - m.c > Tuning.COMPARE_CHROMA -> ChromaShift.PALER
            else -> ChromaShift.SAME
        }
        val temperature = if (!chromatic) TemperatureShift.SAME else {
            val closerToWarm = angle(r.hDeg, Tuning.WARM_POLE_DEG) - angle(m.hDeg, Tuning.WARM_POLE_DEG)
            when {
                closerToWarm > Tuning.COMPARE_HUE_DEG -> TemperatureShift.WARMER
                -closerToWarm > Tuning.COMPARE_HUE_DEG -> TemperatureShift.COOLER
                else -> TemperatureShift.SAME
            }
        }
        if (closeness == Closeness.VERY_CLOSE) {
            return ComparisonFacts(closeness, null, LightnessShift.SAME, ChromaShift.SAME, TemperatureShift.SAME)
        }
        return ComparisonFacts(closeness, hueToward, lightness, chroma, temperature)
    }

    /** Écart de teinte signé le plus court, dans ]-180, 180]. */
    internal fun signedHueDelta(from: Double, to: Double): Double {
        val d = ((to - from) % 360 + 360) % 360
        return if (d > 180) d - 360 else d
    }

    private fun angle(a: Double, b: Double): Double = abs(signedHueDelta(a, b))

    /** Première base du cercle rencontrée en tournant depuis [fromDeg] dans le sens donné. */
    private fun neighbor(fromDeg: Double, increasing: Boolean): BaseColor =
        HUE_WHEEL.map { (base, h) ->
            val d = signedHueDelta(fromDeg, h).let { if (increasing) it else -it }
            base to (if (d < MIN_NEIGHBOR_GAP_DEG) d + 360 else d)
        }.minBy { it.second }.first
}
