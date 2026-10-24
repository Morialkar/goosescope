package com.morialkar.goosescope.core.smooth

import com.morialkar.goosescope.core.Tuning
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Shade
import com.morialkar.goosescope.core.classify.ShadeMatcher
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab

/**
 * Nuance affichée en direct (SPEC F24). Même idée que l'hystérésis du nom de base: une autre nuance
 * ne remplace l'actuelle que si elle est plus proche d'au moins [margin]; un changement de base
 * prend effet tout de suite, puisque la base est déjà stabilisée par [Smoother].
 */
class ShadeStabilizer(private val matcher: ShadeMatcher, private val margin: Double = Tuning.SHADE_HYSTERESIS_MARGIN) {
    private var current: Shade? = null

    init {
        require(margin >= 0.0) { "marge négative: $margin" }
    }

    fun update(c: Oklab, base: BaseColor): Shade {
        val best = matcher.match(c, base)
        val shown = current
        val next = when {
            shown == null || shown.base != base -> best
            ColorMath.distance(c, shown.refLab) - ColorMath.distance(c, best.refLab) >= margin -> best
            else -> shown
        }
        current = next
        return next
    }

    fun reset() {
        current = null
    }
}
